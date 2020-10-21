package com.gammaray.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.jvm.internal.Intrinsics

class BroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val service = BatteryMonitorService()
        var str: String? = null
        if (!Intrinsics.areEqual(
                if (intent as Any? != null) intent!!.action else null,
                "android.intent.action.BATTERY_CHANGED" as Any
            )
        ) {
            if (Intrinsics.areEqual(
                    if (intent as Any? != null) intent!!.action else null,
                    "android.intent.action.DATE_CHANGED" as Any
                )
            ) {
                context?.stopService(Intent(context, BatteryMonitorService::class.java))
                if (context != null) {
                    context.startService(Intent(context, BatteryMonitorService::class.java))
                    return
                }
                return
            }
            if (intent != null) {
                str = intent.action
            }
            if (Intrinsics.areEqual(
                    str as Any?,
                    "android.intent.action.BOOT_COMPLETED" as Any
                ) && context != null
            ) {
                context.startService(Intent(context, BatteryMonitorService::class.java))
            }
        }
        if(context!=null)
            service.updateEntry(context)
    }
}
