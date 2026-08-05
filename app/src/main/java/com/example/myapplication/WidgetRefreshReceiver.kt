package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ACTION_REFRESH_WIDGETS

/**
 * Re-renders all placed widgets when the user interacts with the phone or the
 * shared session/progress changes. It deliberately does NOT rotate the word —
 * rotation only happens via explicit taps (app advance or widget tap), so the
 * widget always mirrors the app's shared per-deck word.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> updateAllWidgets(context)
            Intent.ACTION_SCREEN_ON -> updateAllWidgets(context)
            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }
}
