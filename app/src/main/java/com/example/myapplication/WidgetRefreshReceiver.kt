package com.example.myapplication

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ACTION_REFRESH_WIDGETS
import com.example.myapplication.data.WidgetRefreshStateStore

/** Refreshes widgets and rotates them once per boot or screen-off/unlock cycle. */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.e("WORD_GOBLIN_RX", "🔥 Received: ${intent.action} | User: ${android.os.Process.myUid()}")
        val state = WidgetRefreshStateStore(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (state.claimBoot()) rotateWidgetsForDeviceEvent(context)
            }

            Intent.ACTION_SCREEN_OFF -> state.markScreenOff()

            // The screen turned on. If the device is not actually locked
            // (swipe / Smart Lock), rotate now; if it IS locked, USER_PRESENT
            // fires after unlock and rotates then, so no double rotation.
            Intent.ACTION_SCREEN_ON -> {
                val km = context.getSystemService(KeyguardManager::class.java)
                if (km?.isKeyguardLocked() != true) rotateWidgetsForDeviceEvent(context)
            }

            Intent.ACTION_USER_PRESENT -> {
                if (state.consumePostBootUnlockSuppression()) return
                if (state.consumePendingUnlock()) rotateWidgetsForDeviceEvent(context)
            }

            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }
}
