package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Independent per-widget state for the random-word 2x1 widget.
 *
 * Unlike [SessionStore], this store is NOT shared with the app or the other
 * widgets: each widget id keeps its own current word and flipped state, so the
 * widget can show random words without disturbing the app's session.
 */
class RandomWordWidgetStore(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("random_word_widget_state", Context.MODE_PRIVATE)

    fun currentWordName(widgetId: Int): String? =
        prefs.getString(key(KEY_WORD, widgetId), null)

    fun setCurrentWord(widgetId: Int, wordName: String) {
        prefs.edit()
            .putString(key(KEY_WORD, widgetId), wordName)
            .putBoolean(key(KEY_FLIPPED, widgetId), false)
            .apply()
    }

    fun flipped(widgetId: Int): Boolean =
        prefs.getBoolean(key(KEY_FLIPPED, widgetId), false)

    fun setFlipped(widgetId: Int, flipped: Boolean) {
        prefs.edit().putBoolean(key(KEY_FLIPPED, widgetId), flipped).apply()
    }

    private fun key(prefix: String, widgetId: Int): String = "$prefix$widgetId"

    private companion object {
        const val KEY_WORD = "word_"
        const val KEY_FLIPPED = "flipped_"
    }
}
