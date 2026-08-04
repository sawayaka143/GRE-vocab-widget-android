package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.myapplication.data.WordRepository

/**
 * GRE vocab home-screen widget.
 * Shows a random word (with definition/example when available) from the bundled decks.
 * Tap "Next" to show another random word.
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
        if (intent.action == ACTION_NEXT_WORD) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val repository = WordRepository(context)
    val decks = repository.loadDecks()
    val allWords = decks.flatMap { it.words }
    val word = allWords.randomOrNull() ?: return

    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
    views.setTextViewText(R.id.widget_word, word.word)
    views.setTextViewText(
        R.id.widget_definition,
        word.definition ?: "Definition coming soon"
    )
    views.setTextViewText(
        R.id.widget_deck,
        word.deck
    )
    if (word.example != null) {
        views.setTextViewText(R.id.widget_example, "\u201C${word.example}\u201D")
        views.setViewVisibility(R.id.widget_example, android.view.View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.widget_example, android.view.View.GONE)
    }

    val nextIntent = Intent(context, NewAppWidget::class.java).apply {
        action = ACTION_NEXT_WORD
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        nextIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_next, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private const val ACTION_NEXT_WORD = "com.example.myapplication.action.NEXT_WORD"
