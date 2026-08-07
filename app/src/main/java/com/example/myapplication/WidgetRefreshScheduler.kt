package com.example.myapplication

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Owns the optional periodic refresh job. */
internal object WidgetRefreshScheduler {

    private const val WORK_NAME = "background_widget_refresh"
    private const val REFRESH_INTERVAL_MINUTES = 30L

    fun enable(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            REFRESH_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            REFRESH_INTERVAL_MINUTES,
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
