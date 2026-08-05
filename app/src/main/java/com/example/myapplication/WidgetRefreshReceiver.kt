package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ACTION_REFRESH_WIDGETS
import com.example.myapplication.data.WidgetRefreshStateStore

/** Refreshes widgets and rotates them once per boot or screen-off/unlock cycle. */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = WidgetRefreshStateStore(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (state.claimBoot()) rotateWidgetsForDeviceEvent(context)
            }

            Intent.ACTION_SCREEN_OFF -> state.markScreenOff()

            Intent.ACTION_USER_PRESENT -> {
                if (state.consumePostBootUnlockSuppression()) return
                if (state.consumePendingUnlock()) rotateWidgetsForDeviceEvent(context)
            }

            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }
}
