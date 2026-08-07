package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.ACTION_REFRESH_WIDGETS
import com.example.myapplication.data.WidgetRefreshStateStore

/** Refreshes widgets after boot, unlock, or an explicit app refresh request. */
class WidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.e("WORD_GOBLIN_RX", "🔥 Received: ${intent.action} | User: ${android.os.Process.myUid()}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val state = WidgetRefreshStateStore(context)
                if (state.claimBoot()) rotateWidgetsForDeviceEvent(context)
            }

            Intent.ACTION_USER_PRESENT -> {
                // The app process may have been killed before SCREEN_OFF, so
                // unlock handling cannot depend on persisted screen state.
                rotateWidgetsForDeviceEvent(context)
            }

            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }
}
