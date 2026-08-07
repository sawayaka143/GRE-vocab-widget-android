package com.example.myapplication

import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.Deck
import com.example.myapplication.data.DeckSelectionStore
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.RandomWordWidgetStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WidgetRefreshSettingsStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordPicker
import com.example.myapplication.data.WordRepository
import com.example.myapplication.data.WordState

/**
 * GRE vocab home-screen widget, fully synced with the app via [SessionStore].
 *
 * Hybrid word source: the widget shows a random word from the SELECTED decks
 * (like the small widget), but writes it back to the shared session and marks
 * the picked deck as active, so the app mirrors it when opened — and vice versa.
 *
 * - Tap the word area to advance to a new random word from the selected decks.
 * - Tap the bottom bar to flip between the word and its meaning.
 * - Tap "Learned" to mark the word as mastered (synced with the app).
 */
class NewAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Rotate to a new word on every system-triggered widget update. onUpdate()
        // is called by the system on the periodic APPWIDGET_UPDATE schedule and
        // on widget placement. If a recent rotation already claimed the dedup
        // window, still render the requested ids so placement and periodic
        // updates always paint (never leave the placeholder layout).
        if (!rotateWidgetsForDeviceEvent(context)) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val session = SessionStore(context)
        val decks = WordRepository(context).loadDecks()
        val recentStore = RandomWordWidgetStore(context)
        val active = session.activeDeck()
            ?: decks.firstOrNull()?.name
            ?: return

        when (intent.action) {
            ACTION_FLIP -> {
                session.setFlipped(active, !session.flipped(active))
            }

            ACTION_NEXT -> {
                val current = session.currentWordName(active)
                nextRandomWordAcrossDecks(
                    context,
                    selectedDecks(context, decks),
                    current,
                    recentWords = recentStore.recentWords(widgetId),
                    onPicked = { recentStore.pushRecentWord(widgetId, it.word) }
                )?.let {
                    session.setCurrentWord(it.deck, it)
                    session.setActiveDeck(it.deck)
                }
            }

            ACTION_TOGGLE_LEARNED -> {
                // Don't mutate learning progress while the device is locked —
                // accidental pocket taps shouldn't mark words as mastered.
                if (keyguardLocked(context)) return
                val word = session.currentWordName(active) ?: return
                val progress = ProgressStore(context)
                if (progress.stateOf(word) == WordState.MASTERED) {
                    progress.markDidntKnow(word)
                } else {
                    progress.markKnew(word)
                }
            }
        }
        // Shared state changed: re-render every placed widget.
        updateAllWidgets(context)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val session = SessionStore(context)
    val decks = WordRepository(context).loadDecks()
    val selected = selectedDecks(context, decks)
    if (selected.isEmpty()) return

    // The ACTIVE deck is the one the widget just picked/synced; the widget's
    // displayed word is always its session word. Resolve it fresh so tap-exclusion
    // (and flip/learned) target the word actually on screen.
    val activeDeck = session.activeDeck() ?: selected.first().name
    val activeWord = session.currentWordName(activeDeck)
        ?.let { name -> selected.flatMap { it.words }.firstOrNull { it.word == name } }

    // Resolve the word to show: the shared session word for ANY selected deck
    // (hybrid — the random pool is the selected decks), else seed a fresh random
    // word from the selected decks.
    val word = activeWord
        ?: nextRandomWordAcrossDecks(context, selected, null)?.also {
            session.setCurrentWord(it.deck, it)
            session.setActiveDeck(it.deck)
        }
        ?: return
    val flipped = session.flipped(word.deck)
    val wordState = ProgressStore(context).stateOf(word.word)
    val learned = wordState == WordState.MASTERED
    val palette = widgetPalette(context)

    // The original widget is always the full 4x2 card (no responsive compact variant).
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setInt(R.id.widget_root, "setBackgroundResource", palette.backgroundResource)
    views.setInt(R.id.widget_reveal_bar, "setBackgroundResource", palette.revealBackgroundResource)
    views.setTextViewText(R.id.widget_deck, word.deck)
    views.setTextColor(R.id.widget_deck, palette.accentColor)
    views.setTextViewText(R.id.widget_word, word.word)
    views.setTextColor(R.id.widget_word, palette.primaryTextColor)
    views.setTextColor(R.id.widget_definition, palette.secondaryTextColor)
    views.setTextColor(R.id.widget_example, palette.tertiaryTextColor)
    views.setTextColor(R.id.widget_reveal_bar, palette.secondaryTextColor)
    views.setTextColor(R.id.widget_learned, palette.accentColor)
    views.setColorStateList(
        R.id.widget_learned,
        "setButtonTintList",
        ColorStateList.valueOf(palette.accentColor)
    )
    views.setTextColor(R.id.widget_status, widgetStatusColor(context, wordState))
    views.setCompoundButtonChecked(R.id.widget_learned, learned)

    if (flipped) {
        // Back: word + definition + example, bar says "Tap to go back"
        views.setTextViewText(
            R.id.widget_definition,
            word.definition ?: "Definition coming soon"
        )
        views.setViewVisibility(R.id.widget_definition, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_example,
            word.example?.let { "\u201C$it\u201D" } ?: "Example coming soon"
        )
        views.setViewVisibility(R.id.widget_example, View.VISIBLE)
        views.setTextViewText(R.id.widget_reveal_bar, context.getString(R.string.tap_to_go_back))
    } else {
        // Front: word only, bar says "Tap to see meaning →"
        views.setViewVisibility(R.id.widget_definition, View.GONE)
        views.setViewVisibility(R.id.widget_example, View.GONE)
        views.setTextViewText(
            R.id.widget_reveal_bar,
            context.getString(R.string.tap_to_see_meaning)
        )
    }

    // Tap anywhere on the widget (except the bar/checkbox) -> next random word.
    val nextIntent = buildPendingIntent(context, appWidgetId, ACTION_NEXT, 0)
    views.setOnClickPendingIntent(R.id.widget_root, nextIntent)
    views.setOnClickPendingIntent(R.id.widget_middle_section, nextIntent)

    // Tap bottom bar -> flip
    views.setOnClickPendingIntent(
        R.id.widget_reveal_bar,
        buildPendingIntent(context, appWidgetId, ACTION_FLIP, 1)
    )
    // Tap checkbox -> toggle learned
    views.setOnClickPendingIntent(
        R.id.widget_learned,
        buildPendingIntent(context, appWidgetId, ACTION_TOGGLE_LEARNED, 2)
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/** Re-renders every placed widget after shared state changes. */
internal fun updateAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val newIds = manager.getAppWidgetIds(ComponentName(context, NewAppWidget::class.java))
    for (widgetId in newIds) {
        updateAppWidget(context, manager, widgetId)
    }
    val randomIds = manager.getAppWidgetIds(ComponentName(context, RandomWordWidget::class.java))
    for (widgetId in randomIds) {
        updateRandomWordWidget(context, manager, widgetId)
    }
}

internal data class WidgetPalette(
    val backgroundResource: Int,
    val revealBackgroundResource: Int,
    val accentColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val tertiaryTextColor: Int
)

internal fun widgetPalette(context: Context): WidgetPalette {
    val settings = WidgetRefreshSettingsStore(context)
    if (settings.isMagooshTheme()) {
        return if (settings.isDarkTheme()) {
            WidgetPalette(
                backgroundResource = R.drawable.app_widget_background_magoosh_dark,
                revealBackgroundResource = R.drawable.widget_reveal_bar_bg_magoosh_dark,
                accentColor = android.graphics.Color.rgb(107, 63, 160),
                primaryTextColor = android.graphics.Color.WHITE,
                secondaryTextColor = android.graphics.Color.rgb(220, 220, 220),
                tertiaryTextColor = android.graphics.Color.rgb(170, 170, 170)
            )
        } else {
            WidgetPalette(
                backgroundResource = R.drawable.app_widget_background_light,
                revealBackgroundResource = R.drawable.widget_reveal_bar_bg,
                accentColor = android.graphics.Color.rgb(107, 63, 160),
                primaryTextColor = android.graphics.Color.BLACK,
                secondaryTextColor = android.graphics.Color.rgb(51, 51, 51),
                tertiaryTextColor = android.graphics.Color.rgb(102, 102, 102)
            )
        }
    }

    if (settings.isOledTheme()) {
        return WidgetPalette(
            backgroundResource = R.drawable.app_widget_background_oled,
            revealBackgroundResource = R.drawable.widget_reveal_bar_bg_oled,
            accentColor = android.graphics.Color.rgb(242, 243, 245),
            primaryTextColor = android.graphics.Color.WHITE,
            secondaryTextColor = android.graphics.Color.rgb(138, 143, 152),
            tertiaryTextColor = android.graphics.Color.rgb(110, 114, 120)
        )
    }

    return if (settings.isDarkTheme()) {
        WidgetPalette(
            backgroundResource = R.drawable.app_widget_background_dark,
            revealBackgroundResource = R.drawable.widget_reveal_bar_bg_dark,
            accentColor = android.graphics.Color.rgb(197, 198, 202),
            primaryTextColor = android.graphics.Color.WHITE,
            secondaryTextColor = android.graphics.Color.rgb(193, 198, 198),
            tertiaryTextColor = android.graphics.Color.rgb(193, 198, 198)
        )
    } else {
        WidgetPalette(
            backgroundResource = R.drawable.app_widget_background_light,
            revealBackgroundResource = R.drawable.widget_reveal_bar_bg_light,
            accentColor = android.graphics.Color.rgb(53, 104, 89),
            primaryTextColor = android.graphics.Color.rgb(23, 32, 28),
            secondaryTextColor = android.graphics.Color.rgb(63, 74, 69),
            tertiaryTextColor = android.graphics.Color.rgb(96, 110, 103)
        )
    }
}

/** Rotates each widget's current word after the device wakes or finishes booting. */
internal fun rotateWidgetsForDeviceEvent(context: Context): Boolean {
    if (!com.example.myapplication.data.WidgetRefreshStateStore(context).claimRecentRefresh()) return false

    val session = SessionStore(context)
    val decks = WordRepository(context).loadDecks()
    val selected = selectedDecks(context, decks)
    val manager = AppWidgetManager.getInstance(context)
    val store = RandomWordWidgetStore(context)
    if (selected.isNotEmpty()) {
        // Pick ONE new word for the shared session (all main widgets show this).
        val activeName = session.activeDeck() ?: selected.first().name
        val exclude = session.currentWordName(activeName)
        
        nextRandomWordAcrossDecks(
            context,
            selected,
            exclude,
            onPicked = { word ->
                // Record recency for all main widget instances.
                val mainIds = manager.getAppWidgetIds(ComponentName(context, NewAppWidget::class.java))
                mainIds.forEach { store.pushRecentWord(it, word.word) }
            }
        )?.let {
            session.setCurrentWord(it.deck, it)
            session.setActiveDeck(it.deck)
        }
    }

    val randomIds = manager.getAppWidgetIds(ComponentName(context, RandomWordWidget::class.java))
    for (widgetId in randomIds) {
        val current = store.currentWordName(widgetId)
        nextRandomWordAcrossDecks(
            context,
            selected,
            current,
            recentWords = store.recentWords(widgetId),
            onPicked = { store.pushRecentWord(widgetId, it.word) }
        )?.let {
            store.setCurrentWord(widgetId, it.word)
        }
    }
    updateAllWidgets(context)
    return true
}

private fun buildPendingIntent(
    context: Context,
    widgetId: Int,
    action: String,
    requestCodeOffset: Int = 0
): PendingIntent {
    val intent = Intent(context, NewAppWidget::class.java).apply {
        this.action = action
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    }
    // Use unique request codes per action to prevent intent collision in the system cache.
    val requestCode = widgetId * 10 + requestCodeOffset
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** The decks selected (checked) to feed the widget; falls back to all decks if none selected. */
internal fun selectedDecks(context: Context, allDecks: List<Deck>): List<Deck> {
    val selected = DeckSelectionStore(context).selectedDeckNames()
    return if (selected.isEmpty()) allDecks else allDecks.filter { it.name in selected }
}

/**
 * Picks a random word from the union of [decks], GUARANTEED different from
 * [exclude] when any other word exists. WordPicker's recency penalty is soft
 * (a recent word can still win), so we hard-exclude here by retrying.
 *
 * [recentWords] seeds the picker's recency memory (so recently shown words back
 * off across taps), and [onPicked] is invoked with the chosen word so the caller
 * can record it in persisted history.
 */
internal fun nextRandomWordAcrossDecks(
    context: Context,
    decks: List<Deck>,
    exclude: String?,
    recentWords: List<String> = emptyList(),
    onPicked: ((Word) -> Unit)? = null
): Word? {
    if (decks.isEmpty()) return null
    val picker = WordPicker(
        ProgressStore(context)::stateOf,
        initialRecent = recentWords,
        weights = WidgetRefreshSettingsStore(context).stateWeights()
    )
    val candidates = decks.flatMap { it.words }
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()

    // Try each deck; if the pick equals the excluded word, keep trying.
    val shuffled = decks.shuffled()
    for (attempt in 0 until shuffled.size) {
        val deck = shuffled[attempt % shuffled.size]
        val picked = picker.pickNext(deck.words, exclude) ?: continue
        if (picked.word != exclude) {
            onPicked?.invoke(picked)
            return picked
        }
    }
    // Fallback: any word that isn't the excluded one.
    val fallback = candidates.firstOrNull { it.word != exclude } ?: candidates.first()
    onPicked?.invoke(fallback)
    return fallback
}

internal fun widgetStatusColor(context: Context, state: WordState): Int {
    val settings = WidgetRefreshSettingsStore(context)
    return when {
        settings.isOledTheme() -> when (state) {
            WordState.MASTERED -> android.graphics.Color.rgb(242, 243, 245)
            WordState.REVIEWING -> android.graphics.Color.rgb(181, 184, 192)
            WordState.LEARNING -> android.graphics.Color.rgb(154, 160, 168)
            WordState.NEW -> android.graphics.Color.rgb(110, 114, 120)
        }
        settings.isDarkTheme() -> when (state) {
            WordState.MASTERED -> android.graphics.Color.rgb(95, 214, 139)
            WordState.REVIEWING -> android.graphics.Color.rgb(232, 180, 92)
            WordState.LEARNING -> android.graphics.Color.rgb(232, 138, 138)
            WordState.NEW -> android.graphics.Color.rgb(193, 198, 198)
        }
        else -> when (state) {
            WordState.MASTERED -> android.graphics.Color.rgb(48, 185, 97)
            WordState.REVIEWING -> android.graphics.Color.rgb(235, 161, 90)
            WordState.LEARNING -> android.graphics.Color.rgb(192, 117, 113)
            WordState.NEW -> android.graphics.Color.rgb(102, 102, 102)
        }
    }
}

/** True when the device is locked (keyguard showing). */
private fun keyguardLocked(context: Context): Boolean =
    context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked() ?: false

/**
 * Abbreviates a deck name for compact layouts, e.g. "Common Words I" -> "C.W I".
 * Keeps leading initials (one per word) plus a trailing roman numeral/number.
 */
internal fun abbreviateDeck(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return trimmed
    val parts = trimmed.split(Regex("\\s+"))
    val initials = parts.takeWhile { part ->
        !part.matches(Regex("[IVXLC]+"))
    }.mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(".")
    val trailing = parts.dropWhile { part ->
        !part.matches(Regex("[IVXLC]+"))
    }.take(1).joinToString(" ")
    return (initials + if (trailing.isNotEmpty()) " $trailing" else "").trim()
}

private const val ACTION_FLIP = "com.example.myapplication.action.FLIP"
private const val ACTION_NEXT = "com.example.myapplication.action.NEXT"
private const val ACTION_TOGGLE_LEARNED = "com.example.myapplication.action.TOGGLE_LEARNED"
