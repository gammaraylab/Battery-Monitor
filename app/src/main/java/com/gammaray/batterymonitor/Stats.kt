package com.gammaray.batterymonitor

import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class Stats {
    private lateinit var chargingData:ArrayList<StatDataModel>
    private lateinit var dischargingData:ArrayList<StatDataModel>
    private lateinit var statFile: File
    fun timeTillFull(isCharging:Boolean): String {
        if(!isCharging)
            return "phone is not charging"
        if(chargingData.isEmpty())
            return "calculating"
        var avgRate=0.0
        for(i in chargingData)
            avgRate+=i.rate()
        MainActivity.log("charging $avgRate")
        avgRate /= chargingData.size.toDouble()
        MainActivity.log("charging2 $avgRate")
        val time=-(100.0-chargingData.last().endLevel.toDouble())/avgRate
        var hours=(time/60).toInt().toString()
        var minutes=(time%60).toInt().toString()

        if(hours.length==1)
            hours="0$hours"
        if(minutes.length==1)
            minutes="0$minutes"

        return "$hours h:$minutes m"
    }
    fun timeTillEmpty(isCharging:Boolean): String {
        if(isCharging)
            return "phone is charging"
        if(dischargingData.isEmpty())
            return "calculating"
        var avgRate=0.0
        for(i in dischargingData)
            avgRate+=i.rate()
        MainActivity.log("discharging $avgRate")
        avgRate /= dischargingData.size.toDouble()
        MainActivity.log("discharging2 $avgRate")
        val currentRate=dischargingData.last().rate()
        if(currentRate>avgRate)
            avgRate=currentRate
        val time=-(100.0-dischargingData.last().endLevel.toDouble())/avgRate
        var hours=(time/60).toInt().toString()
        var minutes=(time%60).toInt().toString()

        if(hours.length==1)
            hours="0$hours"
        if(minutes.length==1)
            minutes="0$minutes"

        return "$hours h:$minutes m"
    }
    fun timeSinceLastChanged(): String {
        if(dischargingData.isEmpty())
            return "calculating"
        val time=dischargingData.last().startTime
        var minutes=(time%60).toString()
        var hours=(time/60).toString()

        if(hours.length==1)
            hours="0$hours"
        if(minutes.length==1)
            minutes="0$minutes"

        return "$hours:$minutes"
    }

    fun batteryHealth(): String {
        return "100%"
    }
    fun updateStats(rawData: ArrayList<Data>) {
        val charge = ArrayList<StatDataModel>()
        val discharge = ArrayList<StatDataModel>()
        val data = StatDataModel()
        val n: Int = rawData.size
        if(n<2) //IF ENTRIES ARE LESS THAN 2 THEN WE CANNOT CALCULATE THE STATS
            return
        try {
            var isCharging = true
            var first:Int
            val mx = Vector<Int>()
            val mn = Vector<Int>()
            if (rawData[0].level > rawData[1].level){
                isCharging=false
                mx.add(0)
            }
            else if (rawData[0].level < rawData[1].level)
                mn.add(0)
            for (i in 1 until n - 1) {
                if (rawData[i - 1].level > rawData[i].level && rawData[i].level < rawData[i + 1].level)
                    mn.add(i)
                else if (rawData[i - 1].level < rawData[i].level && rawData[i].level > rawData[i + 1].level)
                    mx.add(i)
            }
            if (rawData[n - 1].level > rawData[n - 2].level)
                mx.add(n - 1)
            else if (rawData[n - 1].level < rawData[n - 2].level)
                mn.add(n - 1)

            while (mx.isNotEmpty() && mn.isNotEmpty()) {
                if (isCharging) {
                    first= mn.firstElement()
                    data.startLevel=(rawData[first].level)
                    data.startTime=(rawData[first].hh * 60 + rawData[first].mm)
                    mn.removeAt(0)
                    first= mx.firstElement()
                    data.endLevel=(rawData[first].level)
                    data.endTime=(rawData[first].hh * 60 + rawData[first].mm)
                    charge.add(data)
                }
                else {
                    first= mx.firstElement()
                    data.startLevel=(rawData[first].level)
                    data.startTime=(rawData[first].hh* 60 + rawData[first].mm)
                    mx.removeAt(0)
                    first= mn.firstElement()
                    data.endLevel=(rawData[first].level)
                    data.endTime=(rawData[first].hh * 60 + rawData[first].mm)
                    discharge.add(data)
                }
                isCharging = !isCharging
            }
            chargingData=charge
            dischargingData=discharge
        } catch (e: NoSuchElementException) {
            e.printStackTrace()
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun Double.format()= String.format("%.5G", this.toString())
}
class StatDataModel(
    var startTime: Int = 0,
    var endTime: Int = 0,
    var startLevel: Int = 0,
    var endLevel: Int = 0
) {

    private fun levelChanged():Int {
        return startLevel - endLevel
    }

    private fun timePassed():Int {
        return endTime-startTime
    }

    fun rate():Double {
        return levelChanged().toDouble() / timePassed().toDouble()
    }
}
