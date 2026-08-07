package com.example.myapplication.data

import kotlin.random.Random

/** How often words in each learning state are picked; higher = more often. */
data class StateWeights(
    val new: Int,
    val learning: Int,
    val reviewing: Int,
    val mastered: Int
) {
    companion object {
        val DEFAULT = StateWeights(
            new = 5,
            learning = 4,
            reviewing = 3,
            mastered = 1
        )
    }
}

/** Upper bound for a single state weight; 0 means the state is skipped while other words remain. */
const val MAX_STATE_WEIGHT = 10

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
    private val random: Random = Random.Default,
    private val initialRecent: List<String> = emptyList(),
    private val weights: StateWeights = StateWeights.DEFAULT
) {

    private val recent = ArrayDeque<String>(initialRecent)

    /** Higher weight = appears more often. Tune these to change the feel. */
    fun pickNext(words: List<Word>, exclude: String? = null): Word? {
        if (words.isEmpty()) return null

        // Weight by learning state, then apply the recency penalty.
        val weighted = words.map { word ->
            val base = when (stateOf(word.word)) {
                WordState.NEW -> weights.new
                WordState.LEARNING -> weights.learning
                WordState.REVIEWING -> weights.reviewing
                WordState.MASTERED -> weights.mastered
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
        /** How much to shrink the weight of a word shown within the last [MAX_RECENT] picks. */
        const val RECENCY_PENALTY = 0.5f

        /** How many recent picks are remembered for the penalty. */
        const val MAX_RECENT = 6
    }
}
