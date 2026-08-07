package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ACTION_REFRESH_WIDGETS
import com.example.myapplication.data.WidgetRefreshStateStore

/**
 * Handles boot and explicit app refresh requests.
 *
 * USER_PRESENT is deliberately NOT registered here: it is not exempt from the
 * Android 8+ implicit-broadcast ban, so a manifest-registered receiver would
 * never fire. Screen-wake events are received by [WidgetRefreshService], which
 * is (re)started from BOOT_COMPLETED here.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.e("WORD_GOBLIN_RX", "🔥 Received: ${intent.action} | User: ${android.os.Process.myUid()}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Restart the wake-event service after reboot, then rotate once.
                WidgetRefreshService.start(context)
                val state = WidgetRefreshStateStore(context)
                if (state.claimBoot()) rotateWidgetsForDeviceEvent(context)
            }

            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }
}
