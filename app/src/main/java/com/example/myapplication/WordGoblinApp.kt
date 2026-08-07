package com.example.myapplication

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.myapplication.data.WidgetRefreshSettingsStore

/**
 * Best-effort application-level screen-on receiver.
 *
 * SCREEN_ON/SCREEN_OFF cannot be received via manifest registration on
 * Android 8+ (implicit broadcast ban). Registering SCREEN_ON here preserves
 * the best-effort refresh while the process is alive. USER_PRESENT is handled
 * by WidgetRefreshReceiver so it can also wake a dead process.
 */
class WordGoblinApp : Application() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    val km = context.getSystemService(KeyguardManager::class.java)
                    if (km?.isKeyguardLocked() != true) rotateWidgetsForDeviceEvent(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        val settings = WidgetRefreshSettingsStore(this)
        if (settings.refreshWhileAway()) {
            WidgetRefreshScheduler.ensureScheduled(
                this,
                settings.effectiveBackgroundIntervalMinutes()
            )
        }
    }
}
