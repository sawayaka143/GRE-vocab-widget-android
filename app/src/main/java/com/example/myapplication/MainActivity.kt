package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
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
import com.example.myapplication.data.SessionStore
import com.example.myapplication.data.WidgetRefreshSettingsStore
import com.example.myapplication.data.WordRepository
import com.example.myapplication.ui.DeckListScreen
import com.example.myapplication.ui.FlashcardScreen
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        if (WidgetRefreshSettingsStore(this).refreshWhileAway()) {
            rotateWidgetsForDeviceEvent(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val repository = remember { WordRepository(applicationContext) }
                val progressStore = remember { ProgressStore(applicationContext) }
                val sessionStore = remember { SessionStore(applicationContext) }
                val selectionStore = remember { DeckSelectionStore(applicationContext) }
                val refreshSettings = remember { WidgetRefreshSettingsStore(applicationContext) }
                var selectedDeck by remember { mutableStateOf<Deck?>(null) }
                var showSettings by remember { mutableStateOf(false) }

                val allDecks = repository.loadDecks()
                val selectableNames = allDecks.map { it.name }.toSet()
                if (selectionStore.isEmpty()) {
                    selectableNames.forEach { selectionStore.setSelected(it, true) }
                } else {
                    // Prune selections for decks no longer present in the bundled data.
                    selectionStore.selectedDeckNames()
                        .filterNot { it in selectableNames }
                        .forEach { selectionStore.setSelected(it, false) }
                }

                if (showSettings) {
                    SettingsScreen(
                        settingsStore = refreshSettings,
                        onBack = { showSettings = false }
                    )
                } else if (selectedDeck == null) {
                    DeckListScreen(
                        decks = repository.loadDecks(),
                        progressStore = progressStore,
                        selectionStore = selectionStore,
                        onPracticeDeck = {
                            sessionStore.setActiveDeck(it.name)
                            selectedDeck = it
                        },
                        onOpenSettings = { showSettings = true }
                    )
                } else {
                    FlashcardScreen(
                        deck = selectedDeck!!,
                        progressStore = progressStore,
                        sessionStore = sessionStore,
                        onBack = { selectedDeck = null }
                    )
                }
            }
        }
        requestBatteryOptimizationExemption()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        Toast.makeText(
            this,
            "To keep widgets updating in Low Power Mode, please allow unrestricted battery usage for WordGoblin.",
            Toast.LENGTH_LONG
        ).show()

        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(requestIntent)
        } catch (_: ActivityNotFoundException) {
            openBatteryOptimizationSettings()
        } catch (_: SecurityException) {
            openBatteryOptimizationSettings()
        }
    }

    private fun openBatteryOptimizationSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
