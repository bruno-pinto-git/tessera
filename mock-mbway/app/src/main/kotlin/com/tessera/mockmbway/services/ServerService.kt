package com.tessera.mockmbway.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tessera.mockmbway.R
import com.tessera.mockmbway.activities.MainActivity
import com.tessera.mockmbway.data.MockSibsServer
import com.tessera.mockmbway.data.PaymentExpiryScheduler
import com.tessera.mockmbway.data.PaymentNotifications
import com.tessera.mockmbway.data.Sounder

class ServerService : Service() {

    private val tag = "ServerService"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate — starting foreground SIBS server")
        createChannel()
        startForegroundCompat()
        acquireWakeLock()
        PaymentNotifications.init(applicationContext)
        Sounder.init()
        MockSibsServer.start(applicationContext)
        PaymentExpiryScheduler.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(tag, "onDestroy — stopping foreground SIBS server")
        super.onDestroy()
        PaymentExpiryScheduler.stop()
        MockSibsServer.stop()
        Sounder.destroy()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mockmbway:server").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun startForegroundCompat() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.server_notif_title))
            .setContentText(getString(R.string.server_notif_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.server_notif_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIF_ID = 4242
        private const val CHANNEL_ID = "mbway_server"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ServerService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ServerService::class.java))
        }
    }
}
