package com.example.myapplication.data

data class Word(
    val word: String,
    val definition: String? = null,
    val example: String? = null,
    val deck: String
)

data class Deck(
    val name: String,
    val words: List<Word>
)
