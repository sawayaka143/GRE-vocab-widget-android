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
                    val decks = selectedDecks(context, WordRepository(context).loadDecks())
                    val current = store.currentWordName(widgetId)
                    nextRandomWordAcrossDecks(
                        context,
                        decks,
                        current,
                        recentWords = store.recentWords(widgetId),
                        onPicked = { store.pushRecentWord(widgetId, it.word) }
                    )?.let {
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
    val decks = selectedDecks(context, WordRepository(context).loadDecks())
    if (decks.isEmpty()) return

    // Resolve this widget's stored word; seed a random one if missing/stale.
    val stored = store.currentWordName(appWidgetId)
    val word = decks.flatMap { it.words }.firstOrNull { it.word == stored }
        ?: nextRandomWordAcrossDecks(
            context,
            decks,
            stored,
            recentWords = store.recentWords(appWidgetId),
            onPicked = { store.pushRecentWord(appWidgetId, it.word) }
        )?.also {
            store.setCurrentWord(appWidgetId, it.word)
        }
        ?: return
    val flipped = store.flipped(appWidgetId)
    val wordState = ProgressStore(context).stateOf(word.word)
    val palette = widgetPalette(context)

    val views = RemoteViews(context.packageName, R.layout.widget_random_word)
    views.setInt(R.id.widget_root_random, "setBackgroundResource", palette.backgroundResource)
    views.setTextViewText(R.id.widget_deck, abbreviateDeck(word.deck))
    views.setTextColor(R.id.widget_deck, palette.accentColor)
    views.setTextViewText(R.id.widget_word, word.word)
    views.setTextColor(R.id.widget_word, palette.primaryTextColor)
    views.setTextColor(R.id.widget_definition, palette.secondaryTextColor)
    views.setTextColor(R.id.widget_status, widgetStatusColor(context, wordState))
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
    // Set on both root and word to ensure responsiveness across all launchers.
    val tapIntent = Intent(context, RandomWordWidget::class.java).apply {
        action = ACTION_TAP
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root_random, pendingIntent)
    views.setOnClickPendingIntent(R.id.widget_word, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private const val ACTION_TAP = "com.example.myapplication.action.RANDOM_WORD_TAP"
