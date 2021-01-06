package com.gammaray.batterymonitor

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileProviderService {
    private val dateFormat: SimpleDateFormat= SimpleDateFormat("dd-MMM-yy",Locale.US)
    fun currentFile(context: Context): File {
        val filesDir = context.filesDir
        val file = File(filesDir, dateFormat.format(Date()) + ".txt")
        if (!file.exists() && MainActivity.writePermission)
            file.createNewFile()
        return file
    }
    fun statFile(context:Context):File{
        val filesDir = context.filesDir
        val file = File(filesDir,"stat.txt")
        if (!file.exists() && MainActivity.writePermission)
            file.createNewFile()
        return file
    }
}