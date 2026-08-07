package com.example.myapplication

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Keeps the process alive so the app can react to screen-wake events.
 *
 * SCREEN_ON/SCREEN_OFF/USER_PRESENT cannot be registered in the manifest on
 * Android 8+ (implicit-broadcast ban), and a plain runtime receiver dies with
 * the process. Running this foreground service keeps the process alive and
 * holds the runtime receivers, so widgets rotate on every screen wake even
 * after the OS would otherwise have killed the process.
 *
 * Started from [MainActivity] (app opened by the user) and on
 * BOOT_COMPLETED (reboot). The 2-second dedup in
 * [com.example.myapplication.data.WidgetRefreshStateStore.claimRecentRefresh]
 * prevents the SCREEN_ON/USER_PRESENT pair from rotating twice.
 */
class WidgetRefreshService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Rotate while the screen is off so the next wake already
                    // shows a fresh word.
                    rotateWidgetsForDeviceEvent(context)
                }

                Intent.ACTION_SCREEN_ON -> {
                    val km = context.getSystemService(KeyguardManager::class.java)
                    if (km?.isKeyguardLocked() != true) rotateWidgetsForDeviceEvent(context)
                }

                Intent.ACTION_USER_PRESENT -> {
                    // Fires after the keyguard is dismissed, covering the
                    // locked-screen case that ACTION_SCREEN_ON skips.
                    rotateWidgetsForDeviceEvent(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.widget_service_channel_name),
            NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.widget_service_notification))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "widget_refresh"
        private const val NOTIFICATION_ID = 1001

        /** Starts the service; safe from a foreground context or BOOT_COMPLETED. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WidgetRefreshService::class.java)
            )
        }
    }
}
