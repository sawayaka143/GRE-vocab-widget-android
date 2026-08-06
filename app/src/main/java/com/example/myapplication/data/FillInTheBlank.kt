package com.example.myapplication.data

import kotlin.random.Random

/** A single multiple-choice option for a fill-in-the-blank question. */
data class Choice(
    val word: Word,
    val isCorrect: Boolean
)

/** A fill-in-the-blank question: a sentence with the word blanked, plus 5 choices. */
data class FillInTheBlankQuestion(
    val word: Word,
    val sentence: String,
    val choices: List<Choice>
)

/**
 * Builds a fill-in-the-blank question from the correct [word] and a set of
 * distractor [words]. The inflected form of [word] in its example sentence is
 * replaced with a blank; the correct word plus the distractors (minus any
 * duplicate) are shuffled into 5 choices.
 */
fun makeQuestion(word: Word, distractors: List<Word>, random: Random = Random.Default): FillInTheBlankQuestion {
    val sentence = word.example?.let { blankExample(it, word.word) } ?: ""
    val otherWords = distractors.filter { it.word != word.word }.distinctBy { it.word }.take(4)
    val pool = buildList {
        add(Choice(word = word, isCorrect = true))
        otherWords.forEach { add(Choice(word = it, isCorrect = false)) }
    }.shuffled(random)
    return FillInTheBlankQuestion(word = word, sentence = sentence, choices = pool)
}

/**
 * Replaces the inflected form of [word] in [example] with "_____".
 *
 * The example sentences use the word in a variety of inflected forms (e.g.
 * "demur" -> "demurred", "galvanize" -> "galvanized"). The blanked word must be
 * a single token so choices (which are conjugated forms) fit grammatically, so
 * the whole inflected token is replaced, not just the root.
 *
 * Each alphanumeric run is checked independently, so punctuation attached to
 * the word (em-dash "innocuous—except", double hyphen "temperance--he") is
 * preserved while only the word is blanked.
 */
fun blankExample(example: String, word: String): String {
    val builder = StringBuilder()
    var index = 0
    while (index < example.length) {
        // Skip non-alphanumeric (preserved as-is).
        val start = index
        while (index < example.length && !example[index].isLetterOrDigit()) index++
        if (index > start) builder.append(example, start, index)

        // Capture the alphanumeric run.
        val wordStart = index
        while (index < example.length && example[index].isLetterOrDigit()) index++
        if (index > wordStart) {
            val token = example.substring(wordStart, index)
            builder.append(if (isInflectionOf(token, word)) "_____" else token)
        }
    }
    return builder.toString()
}

/** True when [token] is [word] or an inflected form of it (case-insensitive). */
fun isInflectionOf(token: String, word: String): Boolean {
    val lower = token.lowercase()
    val root = word.lowercase()

    if (lower == root) return true

    // y -> ies (e.g. "belies" from "belie", "qualifies" from "qualify")
    if (root.endsWith("y") && lower == root.dropLast(1) + "ies") return true
    // y -> ied (e.g. "qualified" from "qualify")
    if (root.endsWith("y") && lower == root.dropLast(1) + "ied") return true
    // y -> ier/iest (e.g. "happier" from "happy")
    if (root.endsWith("y") && (lower == root.dropLast(1) + "ier" || lower == root.dropLast(1) + "iest")) return true

    // s/es/ed/d/ing/ly/ally appended directly to the root
    if (lower == root + "s") return true
    if (lower == root + "es") return true
    if (lower == root + "ed") return true
    if (lower == root + "d") return true
    if (lower == root + "ing") return true
    if (lower == root + "ly") return true
    if (lower == root + "ally") return true

    // e-drop: root ending in silent e (e.g. "galvanize" -> "galvanized")
    if (root.endsWith("e") && lower == root.dropLast(1) + "ing") return true
    if (root.endsWith("e") && lower == root.dropLast(1) + "ed") return true
    if (root.endsWith("e") && lower == root.dropLast(1) + "es") return true

    // doubling the final consonant (e.g. "demur" -> "demurred", "gall" -> "galled")
    if (root.length >= 2 && lower == root + root.last() + "ed") return true
    if (root.length >= 2 && lower == root + root.last() + "ing") return true

    // "ity" noun form (e.g. "prodigal" -> "prodigality")
    if (lower == root + "ity") return true

    // "ce" noun form (e.g. "munificent" -> "munificence")
    if (root.endsWith("t") && lower == root.dropLast(1) + "ce") return true

    return false
}
