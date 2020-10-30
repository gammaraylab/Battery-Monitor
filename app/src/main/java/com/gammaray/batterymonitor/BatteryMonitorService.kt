package com.gammaray.batterymonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BatteryMonitorService : Service() {
    private val NOTIFICATION_ID = 415
    private val broadcastReceiver =BroadcastReceiver()
    private val delay:Long = 60000
    private val fileProviderService = FileProviderService()
    private val hourFormat: SimpleDateFormat = SimpleDateFormat("HH", Locale.US)
    private val minuteFormat: SimpleDateFormat = SimpleDateFormat("mm", Locale.US)
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private val parser = LogParser()

    private val runnable=object: Runnable {
        override fun run() {
            Handler(Looper.getMainLooper()).postDelayed(this,delay)
            updateEntry(this@BatteryMonitorService)
//            MainActivity.success(this@BatteryMonitorService,"running",0)
        }
    }

    companion object {
        const val UPDATE_FLAG = "com.gammaray.batterymonitor.BatteryMonitorService"
        private val localIntent: Intent = Intent(UPDATE_FLAG)
        var tmpLevel = -1
    }
    override fun onBind(p0: Intent?): IBinder? {
        updateEntry(this)
        return null
    }
    override fun onCreate() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED")
        intentFilter.addAction("android.intent.action.DATE_CHANGED")
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED")
        registerReceiver(broadcastReceiver, intentFilter)
        createNotificationChannel()
        notificationBuilder = addNotification()
        val systemService: Any = getSystemService(Context.NOTIFICATION_SERVICE)
            notificationManager = systemService as NotificationManager
            val notificationManager2 = notificationManager
            val i = NOTIFICATION_ID
            val builder = notificationBuilder
            notificationManager2.notify(i, builder.build())
//            handler.post(runnable)
            Handler(Looper.getMainLooper()).postDelayed({runnable
            },delay)
    }

    override fun onDestroy() {
        val notificationManager2 = notificationManager
        notificationManager2.cancel(NOTIFICATION_ID)
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent =
            Intent(applicationContext, javaClass)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent = PendingIntent.getService(
                applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val systemService: Any =
            applicationContext
                .getSystemService(Context.ALARM_SERVICE/*NotificationCompat.CATEGORY_ALARM*/)
        (systemService as AlarmManager)[3, SystemClock.elapsedRealtime() + 2000.toLong()] =
            restartServicePendingIntent
        super.onTaskRemoved(rootIntent)
    }

    private fun batteryLevel(context: Context): Int {
        var num: Int=-1
        val batteryStatus = context.registerReceiver(
            null as BroadcastReceiver?,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (batteryStatus != null)
            num = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        return num
    }

    fun updateEntry(context: Context) {
        val file: File = fileProviderService.currentFile(context)
        val level = batteryLevel(context)
        if (level != tmpLevel && level > 0) {
            tmpLevel = level
            val hh: String = hourFormat.format(Date())
            val mm: String = minuteFormat.format(Date())
            val logParser = parser
            logParser.write(file, hh, mm, level)
            context.sendBroadcast(localIntent)
        }
    }

    private fun addNotification(): NotificationCompat.Builder {
        val notificationBuilder2 =
            NotificationCompat.Builder(this, "com.gammaray.batterymonitor.notification_id")
                .setContentTitle("Battery monitor").setContentText("service running")
                .setSmallIcon(R.drawable.ic_notification_small)
        notificationBuilder2.setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(
                    this,
                    MainActivity::class.java
                ), 0
            )
        )
        return notificationBuilder2
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "com.gammaray.batterymonitor.notification_id",
                "com.gammaray.batterymonitor.notification_name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "monitor battery"
            val systemService: Any = getSystemService(Context.NOTIFICATION_SERVICE)
            (systemService as NotificationManager).createNotificationChannel(channel)
        }
    }

}
