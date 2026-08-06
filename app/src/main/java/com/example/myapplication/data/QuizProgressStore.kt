package com.example.myapplication.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.mutableIntStateOf
import java.nio.charset.StandardCharsets

/**
 * Tracks fill-in-the-blank quiz progress per deck: how many questions have been
 * answered in each deck, plus a per-word learning state for the quiz itself.
 *
 * The word-state machine (NEW -> LEARNING -> REVIEWING -> MASTERED and the
 * knew/didn't-know transitions in [knewTransition]/[didntKnowTransition]) mirrors
 * [ProgressStore] exactly, but is stored in a SEPARATE SharedPreferences file
 * ("quiz_progress" vs ProgressStore's "vocab_progress") and under distinct keys,
 * so quiz progress is fully independent of flashcard progress: answering in the
 * quiz never moves flashcard state, and vice versa. If the transition rules in
 * [ProgressStore] change, mirror them here.
 *
 * Every write bumps [revision] (so open Compose screens recompose) and broadcasts
 * [ACTION_REFRESH_WIDGETS] to refresh placed widgets.
 */
class QuizProgressStore(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("quiz_progress", Context.MODE_PRIVATE)

    /** Bumped on every write; observe to force UI recomposition. */
    val revision = mutableIntStateOf(0)

    /** Number of questions answered in [deckName] (correct or not). */
    fun answeredCount(deckName: String): Int =
        prefs.getInt(key(deckName), 0)

    /** Per-word learning state for the quiz (independent of flashcard state). */
    fun stateOf(word: String): WordState =
        WordState.valueOf(
            prefs.getString(KEY_QUIZ_STATE + word, WordState.NEW.name) ?: WordState.NEW.name
        )

    /**
     * Records one answered question in [deckName] for [word]: bumps the deck's
     * answered count and moves [word] along the learning-state machine, exactly
     * like the flashcards do ([ProgressStore.knewTransition] / did-not-know).
     * Call once per answer.
     */
    fun recordResult(deckName: String, word: String, correct: Boolean) {
        val next = if (correct) knewTransition(stateOf(word)) else didntKnowTransition(stateOf(word))
        prefs.edit()
            .putInt(key(deckName), answeredCount(deckName) + 1)
            .putString(KEY_QUIZ_STATE + word, next.name)
            .apply()
        revision.intValue++
        notifyWidgets()
    }

    /** Returns (mastered, reviewing, learning) counts for a deck, from quiz state. */
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

    /** Deck names contain spaces/symbols; Base64-encode so prefs keys stay clean. */
    private fun key(deckName: String): String =
        "answered_" + Base64.encodeToString(
            deckName.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )

    private companion object {
        /** Distinct from [ProgressStore]'s "state_" so quiz/flashcard state never collides. */
        const val KEY_QUIZ_STATE = "qstate_"
    }
}
