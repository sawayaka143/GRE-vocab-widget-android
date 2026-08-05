package com.example.myapplication

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.data.Deck
import com.example.myapplication.data.ProgressStore
import com.example.myapplication.data.QuizProgressStore
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WidgetRefreshStateStore
import com.example.myapplication.data.WordRepository
import com.example.myapplication.ui.DeckListScreen
import com.example.myapplication.ui.FlashcardScreen
import com.example.myapplication.ui.QuizScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    /**
     * Dynamically registered receiver for screen events. SCREEN_ON/SCREEN_OFF
     * cannot be received via manifest registration on Android 8+ (implicit
     * broadcast ban), so we register in code while the app is running. On
     * screen-on, rotate the widget to a new word.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> WidgetRefreshStateStore(context).markScreenOff()
                Intent.ACTION_SCREEN_ON -> {
                    val km = context.getSystemService(KeyguardManager::class.java)
                    if (km?.isKeyguardLocked() != true) {
                        rotateWidgetsForDeviceEvent(context)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerScreenReceiver()
        setContent {
            MyApplicationTheme {
                val repository = remember { WordRepository(applicationContext) }
                val progressStore = remember { ProgressStore(applicationContext) }
                val quizProgressStore = remember { QuizProgressStore(applicationContext) }
                val sessionStore = remember { SessionStore(applicationContext) }
                var selectedDeck by remember { mutableStateOf<Deck?>(null) }

                val deck = selectedDeck
                if (deck == null) {
                    DeckListScreen(
                        decks = repository.loadDecks(),
                        progressStore = progressStore,
                        quizProgressStore = quizProgressStore,
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

    /** Registers the screen receiver once (in onCreate) so it survives screen-off. */
    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
