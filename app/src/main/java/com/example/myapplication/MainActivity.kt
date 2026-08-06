package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.data.Deck
import com.example.myapplication.data.DeckSelectionStore
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.QuizProgressStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WordRepository
import com.example.myapplication.ui.DeckListScreen
import com.example.myapplication.ui.FlashcardScreen
import com.example.myapplication.ui.QuizScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val repository = remember { WordRepository(applicationContext) }
                val progressStore = remember { ProgressStore(applicationContext) }
                val quizProgressStore = remember { QuizProgressStore(applicationContext) }
                val sessionStore = remember { SessionStore(applicationContext) }
                val selectionStore = remember { DeckSelectionStore(applicationContext) }
                var selectedDeck by remember { mutableStateOf<Deck?>(null) }

                // First run: default-select all non-quiz decks so the widget has content.
                // Quiz decks (Fill in the Blank) are never selectable — they share
                // the same words and have no flashcard data.
                val allDecks = repository.loadDecks()
                val selectableNames = allDecks
                    .filterNot { it.name.startsWith("Common Words - Fill in the Blank") }
                    .map { it.name }
                    .toSet()
                if (selectionStore.isEmpty()) {
                    selectableNames.forEach { selectionStore.setSelected(it, true) }
                } else {
                    // Prune any stale quiz-deck selections from before they were disabled.
                    selectionStore.selectedDeckNames()
                        .filterNot { it in selectableNames }
                        .forEach { selectionStore.setSelected(it, false) }
                }

                val deck = selectedDeck
                if (deck == null) {
                    DeckListScreen(
                        decks = repository.loadDecks(),
                        progressStore = progressStore,
                        quizProgressStore = quizProgressStore,
                        selectionStore = selectionStore,
                        onPracticeDeck = {
                            sessionStore.setActiveDeck(it.name)
                            selectedDeck = it
                        }
                    )
                } else if (deck.name.startsWith("Common Words - Fill in the Blank")) {
                    QuizScreen(
                        deck = deck,
                        quizProgressStore = quizProgressStore,
                        onBack = { selectedDeck = null }
                    )
                } else {
                    FlashcardScreen(
                        deck = deck,
                        progressStore = progressStore,
                        sessionStore = sessionStore,
                        onBack = { selectedDeck = null }
                    )
                }
            }
        }
    }
}
