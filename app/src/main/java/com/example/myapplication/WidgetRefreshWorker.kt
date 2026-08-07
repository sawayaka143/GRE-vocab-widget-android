package com.example.myapplication

import android.app.KeyguardManager
import android.os.PowerManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.data.WidgetRefreshSettingsStore

/** Refreshes widgets periodically when the optional background setting is enabled. */
class WidgetRefreshWorker(
    context: android.content.Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        if (!WidgetRefreshSettingsStore(context).refreshWhileAway()) return Result.success()

        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager?.isInteractive != true) return Result.success()

        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) return Result.success()

        rotateWidgetsForDeviceEvent(context)
        return Result.success()
    }
}
