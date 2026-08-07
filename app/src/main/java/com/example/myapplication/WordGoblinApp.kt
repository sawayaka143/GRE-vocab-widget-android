package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.WidgetRefreshSettingsStore

/**
 * Application entry point.
 *
 * Screen-wake receivers (SCREEN_ON/SCREEN_OFF/USER_PRESENT) live in
 * [WidgetRefreshService], which keeps the process alive so they actually fire.
 * This class only keeps the optional periodic refresh job scheduled.
 */
class WordGoblinApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val settings = WidgetRefreshSettingsStore(this)
        if (settings.refreshWhileAway()) {
            WidgetRefreshScheduler.ensureScheduled(
                this,
                settings.effectiveBackgroundIntervalMinutes()
            )
        }
    }
}
