package com.example.myapplication.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

class WidgetRefreshStateStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        "widget_refresh_state",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun claimBoot(): Boolean {
        val bootId = runCatching {
            Settings.Global.getInt(
                appContext.contentResolver,
                Settings.Global.BOOT_COUNT
            ).toLong()
        }.getOrDefault(-1L)

        // If BOOT_COUNT can't be read (SELinux/Android 13+), treat it as an
        // unknown boot and force a rotation instead of suppressing via -1 == -1.
        val unknownBoot = bootId == -1L

        val lastHandledBoot = prefs.getLong(KEY_LAST_HANDLED_BOOT, Long.MIN_VALUE)
        if (!unknownBoot && bootId == lastHandledBoot) return false

        prefs.edit()
            .putLong(KEY_LAST_HANDLED_BOOT, bootId)
            .apply()
        return true
    }

    /** Avoids rotating twice when two system refresh triggers arrive together. */
    fun claimRecentRefresh(windowMillis: Long = 10_000L): Boolean = synchronized(REFRESH_LOCK) {
        val now = SystemClock.elapsedRealtime()
        val lastRefresh = prefs.getLong(KEY_LAST_REFRESH, Long.MIN_VALUE)
        if (lastRefresh != Long.MIN_VALUE &&
            now >= lastRefresh &&
            now - lastRefresh < windowMillis
        ) return false

        prefs.edit().putLong(KEY_LAST_REFRESH, now).apply()
        true
    }

    private companion object {
        val REFRESH_LOCK = Any()
        const val KEY_LAST_HANDLED_BOOT = "last_handled_boot"
        const val KEY_LAST_REFRESH = "last_refresh"
    }
}
