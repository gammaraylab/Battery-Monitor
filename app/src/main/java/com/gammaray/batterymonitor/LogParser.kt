package com.gammaray.batterymonitor

import android.widget.Toast
import com.gammaray.batterymonitor.MainActivity.Companion.instance
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.jvm.internal.Intrinsics

class LogParser {
    fun read(file: File): ArrayList<Data> {
        var i: Int
        var i2: Int
        val dataList = ArrayList<Data>()
        try {
            val str: String =file.readText()
            var i3 = 0
            while (i3 < str.length) {
                var i4 = i3 + 1
                var tmpString = str[i3].toString()
                if (str[i4] != ':') {
                    tmpString += str[i4].toString()
                    i4++
                }
                val i5 = i4 + 1
                val tmpHour = tmpString.toInt()
                val i6 = i5 + 1
                var tmpString2 = str[i5].toString()
                if (str[i6] != ':') {
                    val sb = StringBuilder()
                    sb.append(tmpString2)
                    i = i6 + 1
                    sb.append(str[i6].toString())
                    tmpString2 = sb.toString()
                } else {
                    i = i6
                }
                val i7 = i + 1
                val tmpMinute = tmpString2.toInt()
                val i8 = i7 + 1
                var tmpString3 = str[i7].toString()
                if (str[i8] != '#') {
                    val sb2 = StringBuilder()
                    sb2.append(tmpString3)
                    val i9 = i8 + 1
                    sb2.append(str[i8].toString())
                    tmpString3 = sb2.toString()
                    if (str[i9] != '#') {
                        val sb3 = StringBuilder()
                        sb3.append(tmpString3)
                        i2 = i9 + 1
                        sb3.append(str[i9].toString())
                        tmpString3 = sb3.toString()
                    } else
                        i2 = i9
                } else
                    i2 = i8
                i3 = i2 + 1
                val tmpLevel = tmpString3.toInt()
                val tmp = Data()
                tmp.hh=tmpHour
                tmp.mm=(tmpMinute)
                tmp.level=(tmpLevel)
                dataList.add(tmp)
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(instance, file.name.toString() + " not found", Toast.LENGTH_SHORT).show()
        } catch (e2: NumberFormatException) {
            Toast.makeText(instance, "Invalid entry, clearing cache " + e2.message.toString(), Toast.LENGTH_SHORT).show()
            file.writeText("")
        } catch (e3: StringIndexOutOfBoundsException) {
            Toast.makeText(instance, "Invalid entry, clearing cache " + e3.message.toString(), Toast.LENGTH_SHORT).show()
            file.writeText("")
        } catch (e4: Exception){
            e4.printStackTrace()
        }
        return dataList
    }

    fun write(file: File, hh: String, mm: String, level: Int) {
        try {
            file.appendText(
                "$hh:$mm:$level#")
            if (file.length() > 51200.toLong())
                file.writeText("")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(instance, "Unable to write into " + file.name, Toast.LENGTH_SHORT).show()
        }
    }
}
