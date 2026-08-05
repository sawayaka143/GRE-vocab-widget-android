package com.example.myapplication

import android.app.Application
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
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> WidgetRefreshStateStore(context).markScreenOff()
                Intent.ACTION_SCREEN_ON -> rotateWidgetsForDeviceEvent(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
