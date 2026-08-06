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

    /** Recently shown words for this widget, oldest first. Bounded to [MAX_RECENT]. */
    fun recentWords(widgetId: Int): List<String> =
        prefs.getString(key(KEY_RECENT, widgetId), null)
            ?.split(RECENT_DELIMITER)
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    /** Records a shown word, deduping and keeping only the most recent [MAX_RECENT]. */
    fun pushRecentWord(widgetId: Int, word: String) {
        val updated = (listOf(word) + recentWords(widgetId).filter { it != word }).take(MAX_RECENT)
        prefs.edit().putString(key(KEY_RECENT, widgetId), updated.joinToString(RECENT_DELIMITER)).apply()
    }

    private fun key(prefix: String, widgetId: Int): String = "$prefix$widgetId"

    private companion object {
        const val KEY_WORD = "word_"
        const val KEY_FLIPPED = "flipped_"
        const val KEY_RECENT = "recent_"

        /** Bounded history of shown words per widget, used to seed recency. */
        const val MAX_RECENT = 15

        /** Words can't contain this char (they're single tokens), so it's a safe delimiter. */
        const val RECENT_DELIMITER = "|"
    }
}
