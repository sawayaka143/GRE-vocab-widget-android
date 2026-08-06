package com.example.myapplication.data

import android.content.Context
import android.provider.Settings

class WidgetRefreshStateStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        "widget_refresh_state",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun markScreenOff() {
        prefs.edit().putBoolean(KEY_AWAITING_UNLOCK, true).apply()
    }

    @Synchronized
    fun consumePendingUnlock(): Boolean {
        if (!prefs.getBoolean(KEY_AWAITING_UNLOCK, false)) return false
        prefs.edit().putBoolean(KEY_AWAITING_UNLOCK, false).apply()
        return true
    }

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
            .putBoolean(KEY_SUPPRESS_POST_BOOT_UNLOCK, true)
            .putBoolean(KEY_AWAITING_UNLOCK, false)
            .apply()
        return true
    }

    @Synchronized
    fun consumePostBootUnlockSuppression(): Boolean {
        if (!prefs.getBoolean(KEY_SUPPRESS_POST_BOOT_UNLOCK, false)) return false
        prefs.edit().putBoolean(KEY_SUPPRESS_POST_BOOT_UNLOCK, false).apply()
        return true
    }

    private companion object {
        const val KEY_AWAITING_UNLOCK = "awaiting_unlock"
        const val KEY_LAST_HANDLED_BOOT = "last_handled_boot"
        const val KEY_SUPPRESS_POST_BOOT_UNLOCK = "suppress_post_boot_unlock"
    }
}
