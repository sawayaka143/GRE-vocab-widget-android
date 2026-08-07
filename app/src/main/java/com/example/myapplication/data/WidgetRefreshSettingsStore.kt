package com.example.myapplication.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.mutableIntStateOf
import com.example.myapplication.WidgetRefreshScheduler

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED,
    MAGOOSH
}

const val MIN_REFRESH_INTERVAL_MINUTES = 1
const val MAX_REFRESH_INTERVAL_MINUTES = 1440
const val MIN_BACKGROUND_REFRESH_INTERVAL_MINUTES = 15

/** Stores optional background widget-refresh behavior. */
class WidgetRefreshSettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        "widget_refresh_settings",
        Context.MODE_PRIVATE
    )

    /** Bumped when the setting changes so the settings screen recomposes. */
    val revision = mutableIntStateOf(0)

    fun refreshWhileAway(): Boolean = prefs.getBoolean(KEY_REFRESH_WHILE_AWAY, false)

    fun refreshIntervalMinutes(): Int = prefs.getInt(
        KEY_REFRESH_INTERVAL_MINUTES,
        DEFAULT_REFRESH_INTERVAL_MINUTES
    ).coerceIn(MIN_REFRESH_INTERVAL_MINUTES, MAX_REFRESH_INTERVAL_MINUTES)

    fun effectiveBackgroundIntervalMinutes(): Long = maxOf(
        refreshIntervalMinutes(),
        MIN_BACKGROUND_REFRESH_INTERVAL_MINUTES
    ).toLong()

    fun themeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
    }.getOrDefault(ThemeMode.SYSTEM)

    fun isDarkTheme(): Boolean = when (themeMode()) {
        ThemeMode.SYSTEM -> (appContext.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.OLED -> true
        ThemeMode.MAGOOSH -> (appContext.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun isMagooshTheme(): Boolean = themeMode() == ThemeMode.MAGOOSH

    fun isOledTheme(): Boolean = themeMode() == ThemeMode.OLED

    fun checkboxesOnRight(): Boolean = prefs.getBoolean(KEY_CHECKBOXES_ON_RIGHT, false)

    fun setCheckboxesOnRight(enabled: Boolean) {
        if (enabled == checkboxesOnRight()) return

        prefs.edit().putBoolean(KEY_CHECKBOXES_ON_RIGHT, enabled).apply()
        revision.intValue++
    }

    fun stateWeights(): StateWeights = StateWeights(
        new = prefs.getInt(KEY_WEIGHT_NEW, StateWeights.DEFAULT.new)
            .coerceIn(0, MAX_STATE_WEIGHT),
        learning = prefs.getInt(KEY_WEIGHT_LEARNING, StateWeights.DEFAULT.learning)
            .coerceIn(0, MAX_STATE_WEIGHT),
        reviewing = prefs.getInt(KEY_WEIGHT_REVIEWING, StateWeights.DEFAULT.reviewing)
            .coerceIn(0, MAX_STATE_WEIGHT),
        mastered = prefs.getInt(KEY_WEIGHT_MASTERED, StateWeights.DEFAULT.mastered)
            .coerceIn(0, MAX_STATE_WEIGHT)
    )

    fun setStateWeights(weights: StateWeights) {
        if (weights == stateWeights()) return

        prefs.edit()
            .putInt(KEY_WEIGHT_NEW, weights.new.coerceIn(0, MAX_STATE_WEIGHT))
            .putInt(KEY_WEIGHT_LEARNING, weights.learning.coerceIn(0, MAX_STATE_WEIGHT))
            .putInt(KEY_WEIGHT_REVIEWING, weights.reviewing.coerceIn(0, MAX_STATE_WEIGHT))
            .putInt(KEY_WEIGHT_MASTERED, weights.mastered.coerceIn(0, MAX_STATE_WEIGHT))
            .apply()
        revision.intValue++
        notifyWidgets()
    }

    fun setRefreshWhileAway(enabled: Boolean) {
        if (enabled == refreshWhileAway()) return

        prefs.edit().putBoolean(KEY_REFRESH_WHILE_AWAY, enabled).apply()
        revision.intValue++
        if (enabled) {
            WidgetRefreshScheduler.enable(appContext, effectiveBackgroundIntervalMinutes())
        } else {
            WidgetRefreshScheduler.disable(appContext)
        }
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        val normalized = minutes.coerceIn(MIN_REFRESH_INTERVAL_MINUTES, MAX_REFRESH_INTERVAL_MINUTES)
        if (normalized == refreshIntervalMinutes()) return

        prefs.edit().putInt(KEY_REFRESH_INTERVAL_MINUTES, normalized).apply()
        revision.intValue++
        if (refreshWhileAway()) {
            WidgetRefreshScheduler.enable(appContext, effectiveBackgroundIntervalMinutes())
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (mode == themeMode()) return

        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        revision.intValue++
        notifyWidgets()
    }

    private fun notifyWidgets() {
        appContext.sendBroadcast(
            Intent(ACTION_REFRESH_WIDGETS).setPackage(appContext.packageName)
        )
    }

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 30
        const val KEY_REFRESH_WHILE_AWAY = "refresh_while_away"
        const val KEY_REFRESH_INTERVAL_MINUTES = "refresh_interval_minutes"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_CHECKBOXES_ON_RIGHT = "checkboxes_on_right"
        const val KEY_WEIGHT_NEW = "weight_new"
        const val KEY_WEIGHT_LEARNING = "weight_learning"
        const val KEY_WEIGHT_REVIEWING = "weight_reviewing"
        const val KEY_WEIGHT_MASTERED = "weight_mastered"
    }
}
