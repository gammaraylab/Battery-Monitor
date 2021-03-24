package com.gammaray.batterymonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BatteryMonitorService : Service() {
    private val NOTIFICATION_ID = 415
    private val broadcastReceiver =BroadcastReceiver()
    private val delay:Long = 20000
    private val fileProviderService = FileProviderService()
    private val hourFormat: SimpleDateFormat = SimpleDateFormat("HH", Locale.US)
    private val minuteFormat: SimpleDateFormat = SimpleDateFormat("mm", Locale.US)
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private val parser = LogParser()

    private val channelID="com.gammaray.batterymonitor.notification_id"
    private lateinit var batteryManager: BatteryManager
    private var batteryStatus: Intent?=null

    private val runnable=object: Runnable {
        override fun run() {
            Handler(Looper.getMainLooper()).postDelayed(this,delay)
            updateEntry(this@BatteryMonitorService)
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()

        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        notificationManager =getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        notificationBuilder = NotificationCompat.Builder(this, channelID)
            .setContentTitle("Stats")
            .setContentText("Tap to see graph")
            .setSmallIcon(R.drawable.notification_icon_small)
            .setOngoing(false)
            .setContentIntent(pendingIntent)

        startForeground(1,notificationBuilder.build())
        notificationManager.notify(1,notificationBuilder.build())
        Handler(Looper.getMainLooper()).postDelayed(runnableNotificationUpdater, 1000)

        Handler(Looper.getMainLooper()).postDelayed(runnable, delay)
    }

    override fun onDestroy() {
        notificationManager.cancel(NOTIFICATION_ID)
        unregisterReceiver(broadcastReceiver)
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(runnableNotificationUpdater)
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
        if(tmpLevel<0){
            if(file.exists() && file.length()>0){
                val sb=StringBuilder()
                val line=file.readText().subSequence(file.length().toInt()-4,file.length().toInt()-1).reversed()
                for(i in line){
                    if(i==':')
                        break
                    sb.append(i)
                }
                tmpLevel=sb.toString().toInt()
            }
        }
        if (level != tmpLevel && level > 0) {
            tmpLevel = level
            val hh: String = hourFormat.format(Date())
            val mm: String = minuteFormat.format(Date())
            val logParser = parser
            logParser.write(file, hh, mm, level)
            context.sendBroadcast(localIntent)
        }
    }

    private val runnableNotificationUpdater= object:Runnable {
        val sb=StringBuilder()
        var current=0
        var voltage:Int?=0
        var level:Int?=0
        override fun run() {
            current=-batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)/1000
            voltage=batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            level=batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
            sb.clear()
            sb.append("${current}mA | ")
            sb.append("${voltage}mV | ")
            sb.append("$level%")
            notificationBuilder.setContentTitle(sb)
            notificationManager.notify(1,notificationBuilder.build())
            Handler(Looper.getMainLooper()).postDelayed(this, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel( ){
        val chan = NotificationChannel(channelID,
            "battery monitor service", NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.GREEN
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        chan.description = "monitor battery"
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

}
