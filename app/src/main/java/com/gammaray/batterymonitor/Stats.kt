package com.gammaray.batterymonitor

import java.util.*
import kotlin.collections.ArrayList

class Stats {
    fun timeTillFull(rawData: ArrayList<StatDataModel>): String {
        if(rawData.isEmpty())
            return "empty"
        val rate: Double = rawData.last().rate()
    val sb = StringBuilder()
        sb.append((60.0 * rate / rawData.size.toDouble()).format())
        sb.append("%per hour")
        return "Calculating"
    }
    fun timeTillEmpty(rawData: ArrayList<StatDataModel>): String {
        return "Calculating"
    }
    fun timeSinceLastChanged(): String {
        return "Calculating"
    }
    fun batteryHealth(): String {
        return "100%"
    }
    fun calculateStats(rawData: ArrayList<Data>) {
        val charge = ArrayList<StatDataModel>()
        val discharge = ArrayList<StatDataModel>()
        val data = StatDataModel()
        val mx = Vector<Int>()
        val mn = Vector<Int>()
        if(rawData.isEmpty())
            return
        try {
            val n: Int = rawData.size
            var isCharging = true
            if (rawData[0].level > rawData[1].level) {
                isCharging = false
                mx.add(0)
            } else if (rawData[0].level< rawData[1].level) {
                mn.add(0)
            }
            val i = n - 1
            for (i2 in 1 until i) {
                if (rawData[i2 - 1].level > rawData[i2].level && rawData[i2]
                        .level< rawData[i2 + 1].level
                ) {
                    mn.add(Integer.valueOf(i2))
                } else if (rawData[i2 - 1].level < rawData[i2]
                        .level && rawData[i2].level > rawData[i2 + 1].level
                ) {
                    mx.add(Integer.valueOf(i2))
                }
            }
            if (rawData[n - 1].level > rawData[n - 2].level) {
                mx.add(Integer.valueOf(n - 1))
            } else if (rawData[n - 1].level < rawData[n - 2].level) {
                mn.add(Integer.valueOf(n - 1))
            }
            var first:Int
            while (mx.isNotEmpty() && mn.isNotEmpty()) {
                if (isCharging) {
                    first= mn.firstElement()
                    data.startLevel=(rawData[first].level)
                    data.startTime=(rawData[first].hh * 60 + rawData[first].mm)
                    mn.remove(0)
                    first= mx.firstElement()
                    data.endLevel=(rawData[first].level)
                    data.endTime=(rawData[first].hh * 60 + rawData[first].mm)
                    charge.add(data)
                } else {
                    first= mx.firstElement()
                    data.startLevel=(rawData[first].level)
                    data.startTime=(rawData[first].hh* 60 + rawData[first].mm)
                    mx.remove(0)
                    first= mn.firstElement()
                    data.endLevel=(rawData[first].level)
                    data.endTime=(rawData[first].hh * 60 + rawData[first].mm)
                    discharge.add(data)
                }
                isCharging = !isCharging
            }
//            timeTillFull(charge)
//            timeTillEmpty(discharge)
        } catch (e: NoSuchElementException) {
            e.printStackTrace()
        } catch (e2: IndexOutOfBoundsException) {
            e2.printStackTrace()
        }
    }
    private fun Double.format()= String.format("%.5G",this.toString())
}
class StatDataModel {
    var endLevel:Int=0
    var endTime=0
    var startLevel=0
    var startTime=0

    fun levelChanged():Int {
        return startLevel - endLevel
    }
    fun timePassed():Int {
        return startTime - endTime
    }
    fun rate():Double {
        return levelChanged().toDouble() / timePassed().toDouble();
    }
}