package com.example.myapplication.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.mutableIntStateOf
import java.nio.charset.StandardCharsets

/**
 * Single source of truth for the shared flashcard session: the current word and
 * flipped state for each deck, plus which deck is active. The app and the widget
 * both read and write this store, so they stay synchronized.
 *
 * Every write bumps [revision] (so open Compose screens recompose) and broadcasts
 * [ACTION_REFRESH_WIDGETS] (so placed widgets re-render).
 */
class SessionStore(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("vocab_session", Context.MODE_PRIVATE)

    /** Bumped on every write; observe to force UI recomposition. */
    val revision = mutableIntStateOf(0)

    fun currentWordName(deckName: String): String? =
        prefs.getString(key(KEY_WORD, deckName), null)

    fun hasWord(deckName: String): Boolean = currentWordName(deckName) != null

    /** Resolves the stored word name to a [Word] within [words]. */
    fun currentWord(deckName: String, words: List<Word>): Word? {
        val name = currentWordName(deckName) ?: return null
        return words.firstOrNull { it.word == name }
    }

    /** Stores a new current word for the deck; a new word always starts unflipped. */
    fun setCurrentWord(deckName: String, word: Word) {
        prefs.edit()
            .putString(key(KEY_WORD, deckName), word.word)
            .putBoolean(key(KEY_FLIPPED, deckName), false)
            .apply()
        revision.intValue++
        notifyWidgets()
    }

    fun flipped(deckName: String): Boolean =
        prefs.getBoolean(key(KEY_FLIPPED, deckName), false)

    fun setFlipped(deckName: String, flipped: Boolean) {
        prefs.edit().putBoolean(key(KEY_FLIPPED, deckName), flipped).apply()
        revision.intValue++
        notifyWidgets()
    }

    fun activeDeck(): String? = prefs.getString(KEY_ACTIVE_DECK, null)

    fun setActiveDeck(deckName: String) {
        prefs.edit().putString(KEY_ACTIVE_DECK, deckName).apply()
        revision.intValue++
        notifyWidgets()
    }

    private fun notifyWidgets() {
        appContext.sendBroadcast(
            Intent(ACTION_REFRESH_WIDGETS).setPackage(appContext.packageName)
        )
    }

    /** Deck names contain spaces/symbols; Base64-encode so prefs keys stay clean. */
    private fun key(prefix: String, deckName: String): String =
        prefix + Base64.encodeToString(
            deckName.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )

    private companion object {
        const val KEY_WORD = "word_"
        const val KEY_FLIPPED = "flipped_"
        const val KEY_ACTIVE_DECK = "activeDeck"
    }
}
