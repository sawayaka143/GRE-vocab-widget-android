package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf

/**
 * App-only UI preferences (not synced to the widget).
 *
 * [revision] is a Compose-observable counter bumped on every write, so open
 * screens recompose and re-read the latest value.
 */
class SettingsStore(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("vocab_settings", Context.MODE_PRIVATE)

    /** Bumped on every write; observe to force UI recomposition. */
    val revision = mutableIntStateOf(0)

    /** Whether the "Tap to go back" bar is shown on the card's back face. */
    fun showFlipBackBar(): Boolean =
        prefs.getBoolean(KEY_SHOW_FLIP_BACK_BAR, true)

    fun setShowFlipBackBar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FLIP_BACK_BAR, enabled).apply()
        revision.intValue++
    }

    private companion object {
        const val KEY_SHOW_FLIP_BACK_BAR = "show_flip_back_bar"
    }
}
