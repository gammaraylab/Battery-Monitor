package com.gammaray.batterymonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.os.postDelayed
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BatteryMonitorService : Service() {
    private val NOTIFICATION_ID = 415
    private val broadcastReceiver =
        BroadcastReceiver()

    /* access modifiers changed from: private */
    private val delay = 60000
    private val fileProviderService = FileProviderService()

    /* access modifiers changed from: private */
    val handler: Handler = Handler()
    private val hourFormat: SimpleDateFormat = SimpleDateFormat("HH", Locale.US)
    private val minuteFormat: SimpleDateFormat = SimpleDateFormat("mm", Locale.US)
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private val parser = LogParser()

    private val runnable= Runnable {
    fun run() {
        this.handler.postDelayed(this,delay)
        val batteryMonitorService: BatteryMonitorService = this.`this$0`
        batteryMonitorService.updateEntry(batteryMonitorService)
    }
    }

    companion object {
        const val UPDATE_FLAG = "com.gammaray.batteryamointor.BatteryMonitorService"
        private val localIntent: Intent = Intent(BatteryMonitorService.Companion.UPDATE_FLAG)

        var tmpLevel = -1
    }
    /*class Companion private constructor() {
        *//* synthetic *//*   constructor(`$constructor_marker`: DefaultConstructorMarker?) : this() {}

        var tmpLevel: Int
            get() = BatteryMonitorService.Companion.tmpLevel
            set(i) {
                BatteryMonitorService.Companion.tmpLevel = i
            }

    }
*/
    override fun onBind(p0: Intent?): IBinder? {
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
            notificationManager2!!.notify(i, builder!!.build())
            handler.post(runnable)
    }

    override fun onDestroy() {
        val notificationManager2 = notificationManager
        notificationManager2!!.cancel(NOTIFICATION_ID)
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return 1
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent =
            Intent(ApplicationProvider.getApplicationContext(), javaClass)
        restartServiceIntent.setPackage(VerifyAccess.getPackageName())
        val restartServicePendingIntent = PendingIntent.getService(
            ApplicationProvider.getApplicationContext(),
            1,
            restartServiceIntent,
            1073741824
        )
        val systemService: Any =
            ApplicationProvider.getApplicationContext()
                .getSystemService(NotificationCompat.CATEGORY_ALARM)
        if (systemService != null) {
            (systemService as AlarmManager)[3, SystemClock.elapsedRealtime() + 2000.toLong()] =
                restartServicePendingIntent
            super.onTaskRemoved(rootIntent)
            return
        }
        throw TypeCastException("null cannot be cast to non-null type android.app.AlarmManager")
    }

    private fun batteryLevel(context: Context): Int {
        var num: Int=-1
        val batteryStatus = context.registerReceiver(
            null as BroadcastReceiver?,
            IntentFilter("android.intent.action.BATTERY_CHANGED")
        )
        if (batteryStatus != null) {
            num = Integer.valueOf(batteryStatus.getIntExtra("level", -1))
        }
        return num
    }

    fun updateEntry(context: Context) {
        val file: File = fileProviderService.currentFile(context)
        val level = batteryLevel(context)
        if (level != BatteryMonitorService.Companion.tmpLevel && level > 0) {
            BatteryMonitorService.Companion.tmpLevel = level
            val hh: String = hourFormat.format(Date())
            val mm: String = minuteFormat.format(Date())
            val logParser = parser
            logParser.write(file, hh, mm, level)
            context.sendBroadcast(BatteryMonitorService.Companion.localIntent)
        }
    }

    private fun addNotification(): NotificationCompat.Builder {
        val notificationBuilder2 =
            NotificationCompat.Builder(this, "com.gammaray.batterymonitor.notification_id")
                .setContentTitle("Battery monitor").setContentText("service running")
                .setSmallIcon(R.drawable.icon_notification)
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
                3
            )
            channel.description = "monitor battery"
            val systemService: Any = getSystemService(Context.NOTIFICATION_SERVICE)
            if (systemService != null) {
                (systemService as NotificationManager).createNotificationChannel(channel)
            }
        }
    }

}
