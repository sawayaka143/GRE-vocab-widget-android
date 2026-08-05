package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences

/** Persists which decks are selected (checked) to feed the widget. */
class DeckSelectionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("deck_selection", Context.MODE_PRIVATE)

    fun selectedDeckNames(): Set<String> =
        prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()

    fun isSelected(deckName: String): Boolean = deckName in selectedDeckNames()

    fun setSelected(deckName: String, selected: Boolean) {
        val current = selectedDeckNames().toMutableSet()
        if (selected) current.add(deckName) else current.remove(deckName)
        prefs.edit().putStringSet(KEY_SELECTED, current).apply()
    }

    /** True if no decks are selected yet (first run) — caller should default-select all. */
    fun isEmpty(): Boolean = selectedDeckNames().isEmpty()

    private companion object {
        const val KEY_SELECTED = "selected_decks"
    }
}
