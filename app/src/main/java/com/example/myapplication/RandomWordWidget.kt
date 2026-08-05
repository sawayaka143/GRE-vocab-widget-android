package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.RandomWordWidgetStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WordRepository

/**
 * Independent 2x1 random-word widget.
 *
 * Shows RANDOM words from the active deck (the deck the user is learning in the
 * app). Tap once -> shows the definition (no example). Tap again -> shows a new
 * random word. Has its own per-widget state ([RandomWordWidgetStore]) and does
 * NOT touch the app session or the other widgets.
 */
class RandomWordWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateRandomWordWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_TAP -> {
                val store = RandomWordWidgetStore(context)
                if (store.flipped(widgetId)) {
                    // Already showing the definition -> show a new random word.
                    val active = activeDeckName(context) ?: return
                    val current = store.currentWordName(widgetId)
                    nextRandomWordForDeck(context, active, current)?.let {
                        store.setCurrentWord(widgetId, it.word)
                    }
                } else {
                    // Showing the word -> flip to the definition.
                    store.setFlipped(widgetId, true)
                }
                // Re-render only this widget (independent of the others).
                updateRandomWordWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
        }
    }
}

internal fun updateRandomWordWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val store = RandomWordWidgetStore(context)
    val active = activeDeckName(context) ?: return
    val decks = WordRepository(context).loadDecks()
    val deck = decks.firstOrNull { it.name == active } ?: return

    // Resolve this widget's stored word; seed a random one if missing/stale.
    val stored = store.currentWordName(appWidgetId)
    val word = deck.words.firstOrNull { it.word == stored }
        ?: nextRandomWordForDeck(context, active, stored)?.also {
            store.setCurrentWord(appWidgetId, it.word)
        }
        ?: return
    val flipped = store.flipped(appWidgetId)
    val wordState = ProgressStore(context).stateOf(word.word)

    val views = RemoteViews(context.packageName, R.layout.widget_random_word)
    views.setTextViewText(R.id.widget_deck, abbreviateDeck(active))
    views.setTextViewText(R.id.widget_word, word.word)
    views.setTextColor(R.id.widget_status, widgetStatusColor(wordState))
    if (flipped) {
        views.setTextViewText(
            R.id.widget_definition,
            word.definition ?: "Definition coming soon"
        )
        views.setViewVisibility(R.id.widget_definition, View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.widget_definition, View.GONE)
    }

    // Tap anywhere on the tile -> flip to definition, or advance to a new word.
    val tapIntent = Intent(context, RandomWordWidget::class.java).apply {
        action = ACTION_TAP
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    views.setOnClickPendingIntent(
        R.id.widget_root_random,
        PendingIntent.getBroadcast(
            context,
            appWidgetId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/** The deck the user is currently learning in the app (fallback: first deck). */
private fun activeDeckName(context: Context): String? {
    val decks = WordRepository(context).loadDecks()
    return SessionStore(context).activeDeck() ?: decks.firstOrNull()?.name
}

private const val ACTION_TAP = "com.example.myapplication.action.RANDOM_WORD_TAP"
