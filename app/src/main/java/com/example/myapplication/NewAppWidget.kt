package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.ProgressStore
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
                nextRandomWordForDeck(context, active, current)?.let {
                    session.setCurrentWord(active, it)
                }
            }

            ACTION_CYCLE_DECK -> {
                if (decks.isEmpty()) return
                val idx = decks.indexOfFirst { it.name == active }
                val next = decks[(idx + 1) % decks.size].name
                session.setActiveDeck(next)
            }

            ACTION_TOGGLE_LEARNED -> {
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
    val allWords = decks.flatMap { it.words }

    // Resolve the active deck's shared word; seed it on first run.
    val word = session.currentWord(active, allWords)
        ?: nextRandomWordForDeck(context, active, null)?.also {
            session.setCurrentWord(active, it)
        }
        ?: return
    val flipped = session.flipped(active)
    val learned = ProgressStore(context).stateOf(word.word) == WordState.MASTERED

    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.widget_deck, word.deck)
    views.setTextViewText(R.id.widget_word, word.word)
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

    // Tap deck name -> cycle the active deck
    views.setOnClickPendingIntent(
        R.id.widget_deck,
        buildPendingIntent(context, appWidgetId, ACTION_CYCLE_DECK)
    )
    // Tap word -> next random word in the active deck
    views.setOnClickPendingIntent(
        R.id.widget_word,
        buildPendingIntent(context, appWidgetId, ACTION_NEXT)
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

/** Re-renders every placed widget (state is shared, so a change anywhere must refresh all). */
internal fun updateAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(
        ComponentName(context, NewAppWidget::class.java)
    )
    for (widgetId in ids) {
        updateAppWidget(context, manager, widgetId)
    }
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

/** Picks a random word from [deckName], different from [exclude], weighted by learning state. */
internal fun nextRandomWordForDeck(context: Context, deckName: String, exclude: String?): Word? {
    val deck = WordRepository(context).loadDecks().firstOrNull { it.name == deckName } ?: return null
    return WordPicker(ProgressStore(context)::stateOf).pickNext(deck.words, exclude)
}

private const val ACTION_FLIP = "com.example.myapplication.action.FLIP"
private const val ACTION_NEXT = "com.example.myapplication.action.NEXT"
private const val ACTION_CYCLE_DECK = "com.example.myapplication.action.CYCLE_DECK"
private const val ACTION_TOGGLE_LEARNED = "com.example.myapplication.action.TOGGLE_LEARNED"
