package com.gammaray.batterymonitor

import android.content.Context
import java.io.File
import java.text.DateFormat
import java.util.*

 class FileProviderService {
    private val dateFormat = DateFormat.getDateInstance(2)

    fun currentFile(context: Context): File {
        val filesDir = context.filesDir
        val file = File(filesDir, this.dateFormat.format(Date()) + ".txt")
        if (!file.exists() && MainActivity.writePermission)
            file.createNewFile()
        return file
    }
}
