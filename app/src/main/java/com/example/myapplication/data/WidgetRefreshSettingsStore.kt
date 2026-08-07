package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import com.example.myapplication.WidgetRefreshScheduler

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

    fun setRefreshWhileAway(enabled: Boolean) {
        if (enabled == refreshWhileAway()) return

        prefs.edit().putBoolean(KEY_REFRESH_WHILE_AWAY, enabled).apply()
        revision.intValue++
        if (enabled) {
            WidgetRefreshScheduler.enable(appContext)
        } else {
            WidgetRefreshScheduler.disable(appContext)
        }
    }

    private companion object {
        const val KEY_REFRESH_WHILE_AWAY = "refresh_while_away"
    }
}
