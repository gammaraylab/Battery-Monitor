package com.gammaray.batterymonitor

import android.widget.Toast
import com.gammaray.batterymonitor.MainActivity.Companion.instance
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.jvm.internal.Intrinsics

class LogParser {
    fun read(file: File): ArrayList<Data> {
        val dataList = ArrayList<Data>()
        try {
            // Read all lines from the file
            val lines = file.readLines()
            lines.forEach {line ->
                var index = 0
                while (index + 7 <= line.length) {
                    // Extract each 7-character entry
                    val entry = line.substring(index, index + 7)

                    // Parse entry into hh, mm, and level integers
                    val hh = entry.substring(0, 2).toInt()
                    val mm = entry.substring(2, 4).toInt()
                    val level = entry.substring(4, 7).toInt()

                    // Create a temporary Data object with the parsed values
                    val tmpData = Data()
                    tmpData.hh = hh
                    tmpData.mm = mm
                    tmpData.level = level

                    // Add the tmpData object to the dataList
                    dataList.add(tmpData)

                    // Move to the next 7-character block
                    index += 7
                }
            }
        }catch (e: FileNotFoundException) {
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
        val formattedLevel = level.toString().padStart(3, '0')
        try {
            file.appendText(
                "$hh$mm$formattedLevel")
            if (file.length() > 51200L) //if file size exceeds 50KB limit clear it
                file.writeText("")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(instance, "Unable to write into " + file.name, Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
            e.printStackTrace()
        }
    }
}
