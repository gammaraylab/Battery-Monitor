package com.gammaray.batterymonitor

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class BatteryLevelMarker(context: Context?, layoutResource: Int) :
    MarkerView(context, layoutResource) {

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        var minutes=""
        var time=""
        val level = e?.y?.toInt()
        val tmp = e?.x?.toInt()
        if (tmp != null) {
            minutes = if (tmp % 60 < 10) {
                val sb = StringBuilder()
                sb.append('0')
                sb.append(tmp % 60)
                sb.toString()
            } else {
                (tmp % 60).toString()
            }
        }
        if (tmp != null) {
            time = if (tmp < 60) {
                "12:" + minutes + "AM"
            } else if (tmp < 720) {
                "${tmp / 60}:${minutes}AM"
            } else if (720 <= tmp && 779 >= tmp) {
                "12:" + minutes + "PM"
            } else {
                val sb2 = StringBuilder()
                sb2.append(tmp / 60 - 12)
                sb2.append(':')
                sb2.append(minutes)
                sb2.append("PM")
                sb2.toString()
            }
        }
        val textView = findViewById<TextView>(R.id.markerText)
        textView.text = "$level%\n$time"
        super.refreshContent(e, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        return MPPointF(
            (-width).toFloat() / 2.0f,
            (-height).toFloat() - 10.0f
        )
    }
}
