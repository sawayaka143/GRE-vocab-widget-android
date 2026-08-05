package com.example.myapplication

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.myapplication.data.WidgetRefreshStateStore

/**
 * Application-level screen-event receiver.
 *
 * SCREEN_ON/SCREEN_OFF cannot be received via manifest registration on
 * Android 8+ (implicit broadcast ban), and registering from an Activity is
 * fragile because the activity (and its receiver) is destroyed when the app
 * goes to the background. Registering here keeps the receiver alive for the
 * whole process lifetime, so the widget rotates on every screen-off/on cycle
 * even when the user is on the home screen.
 */
class WordGoblinApp : Application() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = WidgetRefreshStateStore(context)
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> state.markScreenOff()
                Intent.ACTION_SCREEN_ON -> {
                    val km = context.getSystemService(KeyguardManager::class.java)
                    if (km?.isKeyguardLocked() != true) rotateWidgetsForDeviceEvent(context)
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (state.consumePendingUnlock()) rotateWidgetsForDeviceEvent(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
    }
}
