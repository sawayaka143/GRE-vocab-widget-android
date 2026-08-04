package com.example.myapplication.data

import kotlin.random.Random

/**
 * Picks the next word to practice using weighted randomness driven by learning state.
 *
 * Flow:
 *  - NEW words appear most often (they haven't been practiced).
 *  - LEARNING words resurface regularly until known.
 *  - REVIEWING words appear occasionally for reinforcement.
 *  - MASTERED words are skipped entirely while any non-mastered word remains;
 *    if everything is mastered, they cycle through as review.
 *
 * Recently shown words get a soft recency penalty (their weight is reduced), so a
 * just-seen word typically backs off for a few picks rather than repeating in a row.
 * The penalty is never a hard exclusion — on a small deck a recent word can still
 * win, avoiding deadlocks.
 */
class WordPicker(
    private val stateOf: (String) -> WordState,
    private val random: Random = Random.Default
) {

    private val recent = ArrayDeque<String>()

    /** Higher weight = appears more often. Tune these to change the feel. */
    fun pickNext(words: List<Word>, exclude: String? = null): Word? {
        if (words.isEmpty()) return null

        // Weight by learning state, then apply the recency penalty.
        val weighted = words.map { word ->
            val base = when (stateOf(word.word)) {
                WordState.NEW -> NEW_WEIGHT
                WordState.LEARNING -> LEARNING_WEIGHT
                WordState.REVIEWING -> REVIEWING_WEIGHT
                WordState.MASTERED -> MASTERED_WEIGHT
            }
            val penalized = if (word.word in recent) (base * RECENCY_PENALTY).toInt() else base
            // Never drop below 1 for non-mastered words, so a recent word stays eligible.
            val weight = if (base > 0 && penalized < 1) 1 else penalized
            word to weight
        }

        val available = weighted.filter { (word, weight) ->
            word.word != exclude && weight > 0
        }

        // Fallback: if nothing is eligible (e.g. all mastered), pick any word except the excluded one.
        val pool = if (available.isNotEmpty()) available else weighted.filter { (word, _) -> word.word != exclude }

        if (pool.isEmpty()) return null

        val totalWeight = pool.sumOf { it.second }
        val picked = if (totalWeight > 0) {
            var roll = random.nextInt(totalWeight)
            pool.firstOrNull { (_, weight) ->
                roll -= weight
                roll < 0
            }?.first ?: pool.first().first
        } else {
            // Every candidate is mastered (weight 0): cycle through them uniformly.
            // nextInt(0) would throw, so pick an index instead.
            pool[random.nextInt(pool.size)].first
        }

        // Remember this pick (bounded), so it backs off for the next few selections.
        recent.remove(picked.word)
        recent.addLast(picked.word)
        while (recent.size > MAX_RECENT) recent.removeFirst()

        return picked
    }

    private companion object {
        const val NEW_WEIGHT = 5
        const val LEARNING_WEIGHT = 3
        const val REVIEWING_WEIGHT = 2
        const val MASTERED_WEIGHT = 0

        /** How much to shrink the weight of a word shown within the last [MAX_RECENT] picks. */
        const val RECENCY_PENALTY = 0.5f

        /** How many recent picks are remembered for the penalty. */
        const val MAX_RECENT = 6
    }
}
