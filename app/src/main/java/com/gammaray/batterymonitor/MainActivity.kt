package com.gammaray.batterymonitor

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.jvm.internal.Intrinsics

class MainActivity : AppCompatActivity() {
companion object {
    const val READ_REQUEST_CODE = 45
    var instance: MainActivity? = null
    var writePermission: Boolean=false
    fun errorHandler(site: String?, error: String) {
        Log.e(site, error)
        Toast.makeText(instance, error, Toast.LENGTH_SHORT).show()
    }

    fun logger(message: String, tag: String?) {
        Log.e(tag, message)
    }

    fun error(instance: Context, message: String, length: Int) {
        Log.e("ERROR", message)
        Toasty.error(instance, message, length).show()
    }

    fun warn(instance: Context, message: String, length: Int) {
        Intrinsics.checkParameterIsNotNull(message, "message")
        Log.e("WARNING", message)
        Toasty.warning(instance, message, length).show()
    }

    fun success(instance: Context, message: String, length: Int) {
        Log.e("SUCCESS", message)
        Toasty.success(instance, message, length).show()
    }
}
    init {
        instance = this
    }
private val monthList = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
var watchingHistory = false
var writePermission = false
lateinit var batteryManager: BatteryManager
var batteryStatusIntent: Intent? = null
private val broadcastReceiver=BroadcastReceiver()
private lateinit var chart: LineChart
private lateinit var lineData: LineData
private val fileProviderService = FileProviderService()
//private val quickUpdate=

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
//    quickUpdate.start()
    this.batteryStatusIntent = registerReceiver(broadcastReceiver,IntentFilter("android.intent.action.BATTERY_CHANGED"))
    initialize()
}

override fun onResume() {
//    quickUpdate.start()
    registerReceiver(this.broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
    if (!watchingHistory) {
        drawChart()
    }
    super.onResume()
}
override fun onDestroy() {
    unregisterReceiver(this.broadcastReceiver)
    super.onDestroy()
}
override fun onPause() {
//    this.quickUpdate.cancel()
    super.onPause()
}
override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>,grantResults: IntArray) {
    when {
        requestCode != READ_REQUEST_CODE -> {
            super.onRequestPermissionsResult(requestCode, permission, grantResults)
        }
        grantResults[0] != 0 -> {
            requestPermissions(
                this,
                arrayOf(
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                ),
                READ_REQUEST_CODE
            )
        }
        else -> {
            finish()
            startActivity(intent)
        }
    }
}

override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    if (!checkPermissions())
        return false
    menuInflater.inflate(R.menu.menu, menu)
    return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val itemId: Int = item.itemId
    if (itemId == R.id.exit)
        finish()
    else if (itemId == R.id.history) {
        val calendar: Calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val dpd = DatePickerDialog(this, { _, year1, monthOfYear, dayOfMonth ->
            val monthNew: String = monthList[monthOfYear]
            val sb = java.lang.StringBuilder()
            if (dayOfMonth.toString().length == 1) {
                sb.append('0')
                sb.append(dayOfMonth)
            } else
                sb.append(dayOfMonth.toString())

            val filesDir: File = this.filesDir
            val file = File(filesDir, "$sb-$monthNew-$year1.txt")
            if (file.exists()) {
                this.drawChart(file)
                watchingHistory=true
                watchCurrentData.visibility = View.VISIBLE
            }
            else{
                warn(this,"No entry available",0)
                watchingHistory=false
            }

        }, year, month, day)

        dpd.show()
    }
    return true
}

override fun onBackPressed() {
    finish()
    super.onBackPressed()
}

private fun initialize() {
    registerReceiver(this.broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
    val systemService: Any = getSystemService(Context.BATTERY_SERVICE)
    batteryManager = systemService as BatteryManager
    val marker = BatteryLevelMarker(this, R.layout.marker_view)
    chart = this.findViewById(R.id.chart)
    chart.setDrawGridBackground(false)
    chart.marker = marker
    if (checkPermissions()) {
        startService(Intent(this, BatteryMonitorService::class.java))
        writePermission = true
        if (!watchingHistory) {
            drawChart()
            return
        }
        return
    }
    error( this, "give storage access",0)
    requestPermissions()
}

    fun drawChart(file:File = fileProviderService.currentFile(this)) {
        if (writePermission) {
            val parser = LogParser()
            val sb=StringBuilder()
            val name=file.name.replace(".txt","",true)
            currentInstanceTextView.text=name
            try {
                val rawDataList = parser.read(file)
                calculateStats(rawDataList)
                val entries = ArrayList<Entry>()
                if (rawDataList.isNotEmpty()) {
                    val it = rawDataList . iterator ()
                    var averageLevel = 0
                    while (!it.hasNext()) {
                        val i = it.next()
                        averageLevel += i.level
                        entries.add(Entry(((i.hh * 60) + i.mm).toFloat(),  i.level.toFloat()))
                    }
                    try {
//                        val size = averageLevel / rawDataList.size
                    } catch ( e:ArithmeticException) {
                        e.printStackTrace()
                    }
                    sb.clear()
                    sb.append(rawDataList.last().level)
                    sb.append("%")
                    batteryLevel.text = sb
                    val dataSet = LineDataSet(entries, "Battery level")
                    dataSet.lineWidth = 1.0f
                    dataSet.setDrawValues(false)
                    dataSet.setDrawCircles(false)
                    dataSet.setDrawFilled(true)
                    dataSet.fillColor = R.color.colorFillGraph
                    dataSet.color = R.color.colorGraphLine
                    dataSet.mode = LineDataSet.Mode.LINEAR
                    this.lineData = LineData(dataSet)
                    val lineChart:LineChart = this.chart
                    val xAxis:XAxis
                    val yAxis:YAxis = lineChart.axisLeft
                    yAxis.granularity = 10.0f

                    yAxis.gridColor = R.color.colorGraphGrid
                    val yAxisRight = chart.axisRight
                    if (yAxisRight != null) {
                        yAxisRight.isEnabled = false;
                        xAxis = chart.xAxis
                        xAxis?.setDrawLabels(false)
                        chart.data = this.lineData
                        xAxis.setDrawGridLines(false);
                        chart.invalidate();
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace();
                error( this, "MainActivity.drawChart() \ncannot read from " + file.name.toString(), 0)
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                sb.append("not calculated yet")
                batteryVoltage.text=sb
                batteryLevel.text=sb
                sb.clear()
                error( this, "no Entry present in the system", 0)
            }
        }
    }
private fun checkPermissions(): Boolean {
    val permissionRead =
        ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
    val permissionWrite =
        ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")
    return permissionRead == 0 && permissionWrite == 0
}
fun viewHistory(it: View?) {
    watchCurrentData.visibility = View.GONE//VISIBLE
    watchingHistory=false
    drawChart()
}

fun onReceive(context: Context?, intent: Intent?) {
    if (!watchingHistory)
        drawChart()
}
fun onTick(p0: Long) {
    val sb = StringBuilder()
    val getBatteryStatusIntent: Intent? = this.batteryStatusIntent
    sb.append(getBatteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1))
    sb.append("V")
    batteryVoltage.text = sb.toString()
    val batteryManager: BatteryManager = batteryManager
    val num:Int = batteryManager.getIntProperty(2)
    val tmp:Int = -num / 1000
    sb.clear()
    sb.append(tmp)
    sb.append("mA")
    batteryCurrent.text = sb
}

private fun requestPermissions() {
    requestPermissions(
        this,
        arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        ),
        READ_REQUEST_CODE
    )
}

fun timeTillFull(rawData: ArrayList<StatDataModel>): String {
    if(rawData.isEmpty())
        return "empty"
    val rate: Double = rawData.last().rate()
    val sb = StringBuilder()
    sb.append((60.0 * rate / rawData.size.toDouble()).format())
    sb.append("%per hour")
    Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show()
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

private fun calculateStats(rawData: ArrayList<Data>) {
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
        while (!mx.isEmpty() && !mn.isEmpty()) {
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
        timeTillFull(charge)
        timeTillEmpty(discharge)
    } catch (e: NoSuchElementException) {
        e.printStackTrace()
    } catch (e2: IndexOutOfBoundsException) {
        e2.printStackTrace()
    }
}

private fun Double.format()= String.format("%.5G",this.toString())

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
}