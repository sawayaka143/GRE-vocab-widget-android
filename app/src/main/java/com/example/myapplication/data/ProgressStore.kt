package com.example.myapplication.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf

enum class WordState {
    NEW, LEARNING, REVIEWING, MASTERED
}

/** Broadcast sent after any progress change; triggers a widget refresh. */
const val ACTION_REFRESH_WIDGETS = "com.example.myapplication.action.REFRESH_WIDGETS"

/**
 * Persists per-word learning state using SharedPreferences.
 * Magoosh-style transitions:
 *  - knew:      NEW -> MASTERED, LEARNING -> REVIEWING, REVIEWING -> MASTERED
 *  - didn't know: NEW -> LEARNING, LEARNING/REVIEWING -> LEARNING, MASTERED -> LEARNING
 *
 * [revision] is a Compose-observable counter bumped on every state change, so
 * screens (app or widget-originated) can recompose and re-read fresh counts.
 */
class ProgressStore(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("vocab_progress", Context.MODE_PRIVATE)

    /** Bumped on every state change; observe to force UI recomposition. */
    val revision = mutableIntStateOf(0)

    fun stateOf(word: String): WordState =
        WordState.valueOf(prefs.getString(KEY_STATE + word, WordState.NEW.name) ?: WordState.NEW.name)

    fun markKnew(word: String) {
        prefs.edit().putString(KEY_STATE + word, knewTransition(stateOf(word)).name).apply()
        revision.intValue++
        notifyWidgets()
    }

    fun markDidntKnow(word: String) {
        prefs.edit().putString(KEY_STATE + word, didntKnowTransition(stateOf(word)).name).apply()
        revision.intValue++
        notifyWidgets()
    }

    /** Returns (mastered, reviewing, learning) counts for a deck. */
    fun countsFor(deck: Deck): Triple<Int, Int, Int> {
        var mastered = 0
        var reviewing = 0
        var learning = 0
        for (word in deck.words) {
            when (stateOf(word.word)) {
                WordState.MASTERED -> mastered++
                WordState.REVIEWING -> reviewing++
                WordState.LEARNING -> learning++
                WordState.NEW -> {}
            }
        }
        return Triple(mastered, reviewing, learning)
    }

    private fun knewTransition(state: WordState): WordState = when (state) {
        WordState.NEW -> WordState.MASTERED
        WordState.LEARNING -> WordState.REVIEWING
        WordState.REVIEWING -> WordState.MASTERED
        WordState.MASTERED -> WordState.MASTERED
    }

    private fun didntKnowTransition(state: WordState): WordState = when (state) {
        WordState.NEW -> WordState.LEARNING
        WordState.LEARNING -> WordState.LEARNING
        WordState.REVIEWING -> WordState.LEARNING
        WordState.MASTERED -> WordState.LEARNING
    }

    /** Tell placed widgets to re-render (and rotate to a fresh word) after a state change. */
    private fun notifyWidgets() {
        appContext.sendBroadcast(
            Intent(ACTION_REFRESH_WIDGETS).setPackage(appContext.packageName)
        )
    }

    private companion object {
        const val KEY_STATE = "state_"
    }
}
