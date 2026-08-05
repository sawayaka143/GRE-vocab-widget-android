package com.example.myapplication

import android.app.KeyguardManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.Deck
import com.example.myapplication.data.DeckSelectionStore
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.RandomWordWidgetStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordPicker
import com.example.myapplication.data.WordRepository
import com.example.myapplication.data.WordState

/**
 * GRE vocab home-screen widget, fully synced with the app via [SessionStore].
 *
 * The widget shows the ACTIVE deck's shared word + flipped state. Any tap writes
 * back to the shared session (and to ProgressStore for learned), so the app mirrors
 * it when opened — and vice versa.
 *
 * - Tap the word to advance the active deck to a new random word.
 * - Tap the bottom bar to flip between the word and its meaning.
 * - Tap the deck name to cycle the active deck.
 * - Tap "Learned" to mark the word as mastered (synced with the app).
 */
class NewAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Rotate to a new word on every system-triggered widget update. onUpdate()
        // is called by the system when the screen turns on and on the periodic
        // APPWIDGET_UPDATE schedule — NOT subject to the Samsung broadcast
        // delivery issue that blocks USER_PRESENT/SCREEN_ON. No gap check: each
        // callback rotates, which is exactly "turn screen off/on -> new word".
        if (!keyguardLocked(context)) {
            rotateWidgetsForDeviceEvent(context)
        }

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
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
        val active = session.activeDeck()
            ?: decks.firstOrNull()?.name
            ?: return

        when (intent.action) {
            ACTION_FLIP -> {
                session.setFlipped(active, !session.flipped(active))
            }

            ACTION_NEXT -> {
                val current = session.currentWordName(active)
                nextRandomWordAcrossDecks(context, decks, current)?.let {
                    session.setCurrentWord(it.deck, it)
                }
            }

            ACTION_CYCLE_DECK -> {
                if (decks.isEmpty()) return
                val selected = selectedDecks(context, decks)
                val idx = selected.indexOfFirst { it.name == active }
                val next = selected[(idx + 1) % selected.size].name
                session.setActiveDeck(next)
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
    val active = session.activeDeck() ?: decks.firstOrNull()?.name ?: return
    val selected = selectedDecks(context, decks)
    if (selected.isEmpty()) return

    // Resolve the word to show: the active deck's shared word if that deck is
    // selected, otherwise a fresh random word from the selected decks.
    val word = session.currentWord(active, selected.flatMap { it.words })
        ?: nextRandomWordAcrossDecks(context, selected, null)?.also {
            session.setCurrentWord(it.deck, it)
        }
        ?: return
    val flipped = session.flipped(word.deck)
    val wordState = ProgressStore(context).stateOf(word.word)
    val learned = wordState == WordState.MASTERED

    // The original widget is always the full 4x2 card (no responsive compact variant).
    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.widget_deck, word.deck)
    views.setTextViewText(R.id.widget_word, word.word)
    views.setTextColor(R.id.widget_status, widgetStatusColor(wordState))
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

    // Tap word -> next random word in the active deck
    views.setOnClickPendingIntent(
        R.id.widget_word,
        buildPendingIntent(context, appWidgetId, ACTION_NEXT)
    )
    // Tap deck name -> cycle the active deck
    views.setOnClickPendingIntent(
        R.id.widget_deck,
        buildPendingIntent(context, appWidgetId, ACTION_CYCLE_DECK)
    )
    // Tap bottom bar -> flip
    views.setOnClickPendingIntent(
        R.id.widget_reveal_bar,
        buildPendingIntent(context, appWidgetId, ACTION_FLIP)
    )
    // Tap checkbox -> toggle learned
    views.setOnClickPendingIntent(
        R.id.widget_learned,
        buildPendingIntent(context, appWidgetId, ACTION_TOGGLE_LEARNED)
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

/** Rotates each widget's current word after the device wakes or finishes booting. */
internal fun rotateWidgetsForDeviceEvent(context: Context) {
    val session = SessionStore(context)
    val decks = WordRepository(context).loadDecks()
    val selected = selectedDecks(context, decks)
    if (selected.isNotEmpty()) {
        nextRandomWordAcrossDecks(context, selected, session.currentWordName(selected.first().name))?.let {
            session.setCurrentWord(it.deck, it)
        }
    }

    val manager = AppWidgetManager.getInstance(context)
    val store = RandomWordWidgetStore(context)
    val randomIds = manager.getAppWidgetIds(ComponentName(context, RandomWordWidget::class.java))
    for (widgetId in randomIds) {
        val current = store.currentWordName(widgetId)
        nextRandomWordAcrossDecks(context, selected, current)?.let {
            store.setCurrentWord(widgetId, it.word)
        }
    }
    updateAllWidgets(context)
}

private fun buildPendingIntent(context: Context, widgetId: Int, action: String): PendingIntent {
    val intent = Intent(context, NewAppWidget::class.java).apply {
        this.action = action
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    }
    return PendingIntent.getBroadcast(
        context,
        widgetId,
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
 * Picks a random word from the union of [decks] (uniformly random deck choice,
 * then the deck's weighted picker), different from [exclude].
 */
internal fun nextRandomWordAcrossDecks(context: Context, decks: List<Deck>, exclude: String?): Word? {
    if (decks.isEmpty()) return null
    val picker = WordPicker(ProgressStore(context)::stateOf)
    val shuffled = decks.shuffled()
    for (deck in shuffled) {
        picker.pickNext(deck.words, exclude)?.let { return it }
    }
    return null
}

internal fun widgetStatusColor(state: WordState): Int = when (state) {
    WordState.MASTERED -> android.graphics.Color.rgb(48, 185, 97)
    WordState.REVIEWING -> android.graphics.Color.rgb(235, 161, 90)
    WordState.LEARNING -> android.graphics.Color.rgb(192, 117, 113)
    WordState.NEW -> android.graphics.Color.rgb(102, 102, 102)
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
private const val ACTION_CYCLE_DECK = "com.example.myapplication.action.CYCLE_DECK"
private const val ACTION_TOGGLE_LEARNED = "com.example.myapplication.action.TOGGLE_LEARNED"
