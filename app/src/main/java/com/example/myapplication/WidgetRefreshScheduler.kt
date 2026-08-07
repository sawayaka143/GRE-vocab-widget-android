package com.example.myapplication

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Owns the optional periodic refresh job. */
internal object WidgetRefreshScheduler {

    private const val WORK_NAME = "background_widget_refresh"

    fun enable(context: Context, intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun ensureScheduled(context: Context, intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}
