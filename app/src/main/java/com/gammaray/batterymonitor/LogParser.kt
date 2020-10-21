package com.gammaray.batterymonitor

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset
import kotlin.jvm.internal.Intrinsics

class LogParser {
    fun read(file: File): ArrayList<Data> {
        var i: Int
        var i2: Int
        val file2: File = file
        Intrinsics.checkParameterIsNotNull(file2, "file")
        val dataList = ArrayList<Any>()
        try {
            val str: String =
                FilesKt.`readText$default`(file2, null as Charset?, 1, null as Any?)
            var i3 = 0
            while (i3 < str.length) {
                var i4 = i3 + 1
                var tmpString = str[i3].toString()
                if (str[i4] != ':') {
                    tmpString = tmpString + str[i4].toString()
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
                    } else {
                        i2 = i9
                    }
                } else {
                    i2 = i8
                }
                i3 = i2 + 1
                val tmpLevel = tmpString3.toInt()
                val tmp = Data()
                tmp.setHh(tmpHour)
                tmp.setMm(tmpMinute)
                tmp.setLevel(tmpLevel)
                dataList.add(tmp)
            }
        } catch (e: FileNotFoundException) {
            MainActivity.Companion.errorHandler(
                "LogParser.read()",
                file.name.toString().toString() + " not found"
            )
        } catch (e2: NumberFormatException) {
            MainActivity.Companion.errorHandler(
                "LogParser.read()",
                "Invalid entry, clearing cache " + e2.message.toString()
            )
            FilesKt.`writeText$default`(file2, "", null as Charset?, 2, null as Any?)
        } catch (e3: StringIndexOutOfBoundsException) {
            MainActivity.Companion.errorHandler(
                "LogParser.read()",
                "Invalid entry, clearing cache " + e3.message.toString()
            )
            FilesKt.`writeText$default`(file2, "", null as Charset?, 2, null as Any?)
        }
        return dataList
    }

    fun write(file: File, hh: String, mm: String, level: Int) {
        Intrinsics.checkParameterIsNotNull(file, "file")
        Intrinsics.checkParameterIsNotNull(hh, "hh")
        Intrinsics.checkParameterIsNotNull(mm, "mm")
        try {
            FilesKt.`appendText$default`(
                file,
                "$hh:$mm:$level#",
                null as Charset?,
                2,
                null as Any?
            )
            if (file.length() > 15000.toLong()) {
                FilesKt.`writeText$default`(file, "", null as Charset?, 2, null as Any?)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            val companion: MainActivity.Companion = MainActivity.Companion
            companion.errorHandler("LogParser.write()", "Unable to write into " + file.getName())
        }
    }
}
