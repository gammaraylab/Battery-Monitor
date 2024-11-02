package com.gammaray.batterymonitor

import java.util.*
import kotlin.collections.ArrayList

class Stats {
    private lateinit var chargingData:ArrayList<StatDataModel>
    private lateinit var dischargingData:ArrayList<StatDataModel>

    fun timeTillFull(isCharging:Boolean): String {
        var hours:String
        var minutes:String

        if(!isCharging)
            return "phone is not charging"

        try {
            if (chargingData.isEmpty())
                return "calculating"
            var avgRate = 0.0
            for (i in chargingData)
                avgRate += i.rate()
            avgRate /= chargingData.size.toDouble()
            val time = -(100.0 - chargingData.last().endLevel.toDouble()) / avgRate
            hours = (time / 60).toInt().toString()
            minutes = (time % 60).toInt().toString()

            if (hours.length == 1)
                hours = "0$hours"
            if (minutes.length == 1)
                minutes = "0$minutes"
        }catch (e: kotlin.UninitializedPropertyAccessException){
            e.printStackTrace()
            return "--:--"
        }

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
        avgRate /= dischargingData.size.toDouble()
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
        if( dischargingData.isEmpty())
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

    fun updateStats(rawData: ArrayList<Data>) {
        val charge = ArrayList<StatDataModel>()
        val discharge = ArrayList<StatDataModel>()
        val data = StatDataModel()
        val n: Int = rawData.size
        if(n<2) //IF ENTRIES ARE LESS THAN 2 THEN WE CANNOT CALCULATE THE STATS
            return
        try {
            var isCharging = true
            var element:Int
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
                    element= mn.firstElement()
                    data.startLevel=(rawData[element].level)
                    data.startTime=(rawData[element].hh * 60 + rawData[element].mm)
                    mn.removeAt(0)
                    element= mx.firstElement()
                    data.endLevel=(rawData[element].level)
                    data.endTime=(rawData[element].hh * 60 + rawData[element].mm)
                    charge.add(data)
                }
                else {
                    element= mx.firstElement()
                    data.startLevel=(rawData[element].level)
                    data.startTime=(rawData[element].hh* 60 + rawData[element].mm)
                    mx.removeAt(0)
                    element= mn.firstElement()
                    data.endLevel=(rawData[element].level)
                    data.endTime=(rawData[element].hh * 60 + rawData[element].mm)
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
}

class StatDataModel(
    var startTime: Int = 0,
    var endTime: Int = 0,
    var startLevel: Int = 0,
    var endLevel: Int = 0
) {
    fun rate():Double {
        return (startLevel - endLevel).toDouble() / (endTime-startTime).toDouble()
    }
}
