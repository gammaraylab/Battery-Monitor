package com.gammaray.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val service = BatteryMonitorService()
        when (intent?.action) {
            Intent.ACTION_BATTERY_CHANGED -> service.updateEntry(context!!)
            Intent.ACTION_DATE_CHANGED -> {
                context?.stopService(Intent(context, BatteryMonitorService::class.java))
                context?.startService(Intent(context, BatteryMonitorService::class.java))
            }
            Intent.ACTION_BOOT_COMPLETED -> context?.startService(Intent(context, BatteryMonitorService::class.java))
        }
    }
}
