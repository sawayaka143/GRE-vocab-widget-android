package com.example.myapplication.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordPickerTest {

    private fun words(vararg terms: String) = terms.map { Word(word = it, deck = "test") }

    /** Builds a picker whose state lookup returns constants for the given words. */
    private fun picker(
        vararg stateOverrides: Pair<String, WordState>,
        seed: Int = 0,
        weights: StateWeights = StateWeights.DEFAULT
    ): WordPicker {
        val state = stateOverrides.toMap()
        return WordPicker({ state[it] ?: WordState.NEW }, Random(seed), weights = weights)
    }

    @Test
    fun emptyList_returnsNull() {
        assertNull(picker().pickNext(emptyList()))
    }

    @Test
    fun singleWordExcluded_returnsNull() {
        val all = words("alpha")
        assertNull(picker().pickNext(all, exclude = "alpha"))
    }

    @Test
    fun allMastered_doesNotThrow_andReturnsSomeWord() {
        val all = words("alpha", "beta", "gamma")
        val picker = picker(
            "alpha" to WordState.MASTERED,
            "beta" to WordState.MASTERED,
            "gamma" to WordState.MASTERED,
            seed = 7
        )
        val picked = picker.pickNext(all)
        assertNotNull(picked)
    }

    @Test
    fun allMastered_neverReturnsExcluded() {
        val all = words("alpha", "beta", "gamma")
        val mastered = picker(
            "alpha" to WordState.MASTERED,
            "beta" to WordState.MASTERED,
            "gamma" to WordState.MASTERED,
            seed = 3
        )
        repeat(50) {
            assertNotEquals("alpha", mastered.pickNext(all, exclude = "alpha")?.word)
        }
    }

    @Test
    fun masteredWords_areSkippedWhileAnyNonMasteredRemains() {
        val all = words("newWord", "masteredWord")
        val picker = picker(
            "masteredWord" to WordState.MASTERED,
            seed = 1,
            weights = StateWeights(new = 5, learning = 4, reviewing = 3, mastered = 0)
        )
        repeat(50) {
            assertEquals("newWord", picker.pickNext(all)?.word)
        }
    }

    @Test
    fun nonMasteredExcluded_fallsBackToMasteredWord() {
        val all = words("newWord", "masteredWord")
        val picker = picker("masteredWord" to WordState.MASTERED)
        assertEquals("masteredWord", picker.pickNext(all, exclude = "newWord")?.word)
    }

    @Test
    fun bothWordsAppearEventually_notDeadlocked() {
        val all = words("alpha", "beta")
        val picker = picker(seed = 42)
        val seen = mutableSetOf<String>()
        repeat(200) {
            seen += picker.pickNext(all)?.word ?: error("pick returned null")
        }
        assertEquals(setOf("alpha", "beta"), seen)
    }
}