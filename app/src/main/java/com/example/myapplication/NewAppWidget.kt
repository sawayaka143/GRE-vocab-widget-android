package com.example.myapplication

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.Word
import com.example.myapplication.data.WordPicker
import com.example.myapplication.data.WordRepository
import com.example.myapplication.data.WordState

/**
 * GRE vocab home-screen widget.
 * - Tap the word to cycle to a different random word.
 * - Tap the bottom bar to flip between the word and its meaning.
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

        val manager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_FLIP -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val flipped = !prefs.getBoolean(KEY_FLIPPED + widgetId, false)
                prefs.edit().putBoolean(KEY_FLIPPED + widgetId, flipped).apply()
                updateAppWidget(context, manager, widgetId)
            }

            ACTION_NEXT -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getString(KEY_WORD + widgetId, null)
                val next = nextRandomWord(context, current) ?: return
                prefs.edit()
                    .putString(KEY_WORD + widgetId, next.word)
                    .putBoolean(KEY_FLIPPED + widgetId, false)
                    .apply()
                updateAppWidget(context, manager, widgetId)
            }

            ACTION_TOGGLE_LEARNED -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val word = prefs.getString(KEY_WORD + widgetId, null) ?: return
                val progress = ProgressStore(context)
                val isLearned = progress.stateOf(word) == WordState.MASTERED
                if (isLearned) progress.markDidntKnow(word) else progress.markKnew(word)
                updateAppWidget(context, manager, widgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(KEY_FLIPPED + appWidgetId)
            editor.remove(KEY_WORD + appWidgetId)
        }
        editor.apply()
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Pick a word once per widget and remember it, so flips don't re-roll.
    val word = currentWordFor(context, prefs, appWidgetId)
    if (word == null) {
        appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(context.packageName, R.layout.new_app_widget))
        return
    }

    val flipped = prefs.getBoolean(KEY_FLIPPED + appWidgetId, false)
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

    // Tap word -> next random word
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

/** Returns the word this widget is showing, picking and remembering a random one on first use. */
private fun currentWordFor(
    context: Context,
    prefs: android.content.SharedPreferences,
    appWidgetId: Int
): Word? {
    val saved = prefs.getString(KEY_WORD + appWidgetId, null)
    if (saved != null) {
        return WordRepository(context).loadDecks().flatMap { it.words }
            .firstOrNull { it.word == saved }
    }

    val word = nextRandomWord(context, null) ?: return null
    prefs.edit().putString(KEY_WORD + appWidgetId, word.word).apply()
    return word
}

/** Picks a random word different from [exclude], weighted by learning state. */
private fun nextRandomWord(context: Context, exclude: String?): Word? {
    val allWords = WordRepository(context).loadDecks().flatMap { it.words }
    return WordPicker(ProgressStore(context)).pickNext(allWords, exclude)
}

private const val PREFS_NAME = "vocab_widget_prefs"
private const val KEY_FLIPPED = "flipped_"
private const val KEY_WORD = "word_"
private const val ACTION_FLIP = "com.example.myapplication.action.FLIP"
private const val ACTION_NEXT = "com.example.myapplication.action.NEXT"
private const val ACTION_TOGGLE_LEARNED = "com.example.myapplication.action.TOGGLE_LEARNED"
