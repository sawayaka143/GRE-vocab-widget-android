package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WordRepository
import com.example.myapplication.data.WordState

/**
 * 2x1 GRE vocab tile, synced with the app via [SessionStore].
 *
 * Shows the ACTIVE deck's shared word. Tapping the tile flips it to reveal
 * ONLY the definition (no example), and back. The flip state is shared with
 * the app and the full card widget, so they all stay in sync.
 */
class LockScreenWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateLockScreenWidget(context, appWidgetManager, appWidgetId)
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
            ACTION_FLIP -> {
                val session = SessionStore(context)
                val active = session.activeDeck()
                    ?: WordRepository(context).loadDecks().firstOrNull()?.name
                    ?: return
                session.setFlipped(active, !session.flipped(active))
                // Shared state changed: re-render every placed widget.
                updateAllWidgets(context)
            }
        }
    }
}

internal fun updateLockScreenWidget(
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

    val views = RemoteViews(context.packageName, R.layout.widget_lock_screen)
    views.setTextViewText(R.id.widget_deck, abbreviateDeck(word.deck))
    views.setTextViewText(R.id.widget_word, word.word)
    views.setCompoundButtonChecked(
        R.id.widget_learned,
        ProgressStore(context).stateOf(word.word) == WordState.MASTERED
    )
    if (flipped) {
        views.setTextViewText(
            R.id.widget_definition,
            word.definition ?: "Definition coming soon"
        )
        views.setViewVisibility(R.id.widget_definition, View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.widget_definition, View.GONE)
    }

    // Tap anywhere on the tile -> flip word/definition (in-widget, no app launch).
    val flipIntent = Intent(context, LockScreenWidget::class.java).apply {
        action = ACTION_FLIP
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    views.setOnClickPendingIntent(
        R.id.widget_root_lock_screen,
        PendingIntent.getBroadcast(
            context,
            appWidgetId,
            flipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private const val ACTION_FLIP = "com.example.myapplication.action.LOCK_SCREEN_FLIP"
