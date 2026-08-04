package com.example.myapplication.data

import android.content.Context
import org.json.JSONObject

class WordRepository(private val context: Context) {

    private val decks: List<Deck> by lazy { loadFromAsset() }

    fun loadDecks(): List<Deck> = decks

    private fun loadFromAsset(): List<Deck> {
        val raw = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val decksJson = root.getJSONArray("decks")
        return buildList {
            for (i in 0 until decksJson.length()) {
                val deckJson = decksJson.getJSONObject(i)
                val name = deckJson.getString("name")
                val wordsJson = deckJson.getJSONArray("words")
                val words = buildList {
                    for (j in 0 until wordsJson.length()) {
                        val wordJson = wordsJson.getJSONObject(j)
                        add(
                            Word(
                                word = wordJson.getString("word"),
                                definition = wordJson.optString("definition").takeIf { it.isNotEmpty() },
                                example = wordJson.optString("example").takeIf { it.isNotEmpty() },
                                deck = name
                            )
                        )
                    }
                }
                add(Deck(name = name, words = words))
            }
        }
    }
}
