package com.example.myapplication.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FillInTheBlankTest {

    private fun word(term: String, example: String) =
        Word(word = term, definition = "definition of $term", example = example, deck = "test")

    // --- isInflectionOf ---

    @Test
    fun exactWord_isInflection() {
        assertTrue(isInflectionOf("demur", "demur"))
        assertTrue(isInflectionOf("Demur", "demur"))
        assertTrue(isInflectionOf("DEMUR", "demur"))
    }

    @Test
    fun inflectedForms_areDetected() {
        assertTrue(isInflectionOf("demurred", "demur"))
        assertTrue(isInflectionOf("galvanized", "galvanize"))
        assertTrue(isInflectionOf("belies", "belie"))
        assertTrue(isInflectionOf("qualified", "qualify"))
        assertTrue(isInflectionOf("chortling", "chortle"))
        assertTrue(isInflectionOf("mitigating", "mitigate"))
        assertTrue(isInflectionOf("cryptically", "cryptic"))
        assertTrue(isInflectionOf("prodigality", "prodigal"))
        assertTrue(isInflectionOf("munificence", "munificent"))
        assertTrue(isInflectionOf("Aberrations", "aberration"))
        assertTrue(isInflectionOf("idiosyncrasies", "idiosyncrasy"))
    }

    @Test
    fun unrelatedWords_areNotInflections() {
        assertTrue(!isInflectionOf("gallant", "gall"))
        assertTrue(!isInflectionOf("galler", "gall"))
        assertTrue(!isInflectionOf("intentionally", "innocuous"))
        assertTrue(!isInflectionOf("Sally", "gall"))
        assertTrue(!isInflectionOf("finally", "harangue"))
        assertTrue(!isInflectionOf("qualified", "quality"))
    }

    // --- blankExample ---

    @Test
    fun blankExample_replacesInflectedWord() {
        assertEquals(
            "Wallace disliked the cold, so he _____ when his friends suggested they go skiing in the Alps.",
            blankExample(
                "Wallace disliked the cold, so he demurred when his friends suggested they go skiing in the Alps.",
                "demur"
            )
        )
    }

    @Test
    fun blankExample_replacesAllOccurrences() {
        val example = "The artist operated according to a peculiar aesthetic, not considering any photograph " +
            "to be worth publishing unless it contained a marine mammal. The aesthetic of the second film was different."
        val blanked = blankExample(example, "aesthetic")
        // Two occurrences of "aesthetic" both blanked.
        assertEquals(
            "The artist operated according to a peculiar _____, not considering any photograph " +
                "to be worth publishing unless it contained a marine mammal. The _____ of the second film was different.",
            blanked
        )
    }

    @Test
    fun blankExample_preservesPunctuation() {
        // Em-dash attached directly to the word is preserved after the blank.
        assertEquals(
            "Everyone found Nancy's banter _____\u2014except for Mike, who felt like she was intentionally picking on him.",
            blankExample(
                "Everyone found Nancy's banter innocuous\u2014except for Mike, who felt like she was intentionally picking on him.",
                "innocuous"
            )
        )
        // Double hyphen attached.
        assertEquals(
            "Welles wasn't known for his _____--he usually ate enough for two and drank enough for three.",
            blankExample(
                "Welles wasn't known for his temperance--he usually ate enough for two and drank enough for three.",
                "temperance"
            )
        )
        // Apostrophe possessive stays.
        assertEquals(
            "Dexter found the police officer's questions _____ -after all, he thought, did she have to pry?",
            blankExample(
                "Dexter found the police officer's questions impertinent -after all, he thought, did she have to pry?",
                "impertinent"
            )
        )
    }

    @Test
    fun blankExample_noMatch_leavesExampleUnchanged() {
        val example = "The quick brown fox jumps over the lazy dog."
        assertEquals(example, blankExample(example, "nonexistent"))
    }

    // --- makeQuestion ---

    @Test
    fun makeQuestion_hasFiveChoices_oneCorrect() {
        val target = word("demur", "Wallace disliked the cold, so he demurred when his friends suggested they go skiing.")
        val distractors = listOf(
            word("frugal", "Monte was frugal."),
            word("impertinent", "Dexter was impertinent."),
            word("amalgam", "The band was an amalgam."),
            word("gregarious", "Leaders can be gregarious."),
            word("venality", "Officials were accused of venality.")
        )
        val q = makeQuestion(target, distractors, Random(1))
        assertEquals(5, q.choices.size)
        assertEquals(1, q.choices.count { it.isCorrect })
        assertTrue(q.choices.any { it.isCorrect && it.word == target })
        // Sentence blanked.
        assertEquals(
            "Wallace disliked the cold, so he _____ when his friends suggested they go skiing.",
            q.sentence
        )
    }

    @Test
    fun makeQuestion_filtersDuplicateDistractors() {
        val target = word("demur", "Wallace disliked the cold, so he demurred.")
        val distractors = listOf(
            word("frugal", "Monte was frugal."),
            word("frugal", "Monte was frugal again."),
            word("impertinent", "Dexter was impertinent."),
            word("amalgam", "The band was an amalgam."),
            word("gregarious", "Leaders can be gregarious.")
        )
        val q = makeQuestion(target, distractors, Random(2))
        // Correct + 4 unique distractors.
        assertEquals(5, q.choices.size)
        assertEquals(1, q.choices.count { it.isCorrect })
        assertEquals(4, q.choices.filter { !it.isCorrect }.map { it.word.word }.toSet().size)
    }

    @Test
    fun makeQuestion_choicesAreShuffled() {
        val target = word("demur", "Wallace disliked the cold, so he demurred.")
        val distractors = listOf(
            word("frugal", "Monte was frugal."),
            word("impertinent", "Dexter was impertinent."),
            word("amalgam", "The band was an amalgam."),
            word("gregarious", "Leaders can be gregarious.")
        )
        // Two different seeds should produce different orders (with overwhelming probability).
        val order1 = makeQuestion(target, distractors, Random(1)).choices.map { it.word.word }
        val order2 = makeQuestion(target, distractors, Random(2)).choices.map { it.word.word }
        assertNotEquals(order1, order2)
    }
}
