package com.gammaray.batterymonitor

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import es.dmoral.toasty.Toasty
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.jvm.internal.Intrinsics

class MainActivity : AppCompatActivity() {
companion object {
    const val READ_REQUEST_CODE = 45
    var instance: MainActivity? = null
    var writePermission: Boolean=false
    var watchingHistory: Boolean=false
    fun errorHandler(site: String?, error: String) {
        Log.e(site, error)
//        Toast.makeText(this, error, 1).show()
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
private val monthList = arrayOf(
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec"
)
var watchingHistory = false
var writePermission = false
var batteryManager: BatteryManager? = null
var batteryStatusIntent: Intent? = null
private val broadcastReceiver=BroadcastReceiver()
private lateinit var chart: LineChart
private lateinit var lineData: LineData
private val fileProviderService = FileProviderService()
//private val quickUpdate=

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    quickUpdate.start()
    this.batteryStatusIntent = registerReceiver(broadcastReceiver,IntentFilter("android.intent.action.BATTERY_CHANGED"))
    initialize()
}

override fun onResume() {
    quickUpdate.start()
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
    this.quickUpdate.cancel()
    super.onPause()
}
override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>,grantResults: IntArray) {
    when {
        requestCode != 45 -> {
            super.onRequestPermissionsResult(requestCode, permission, grantResults)
        }
        grantResults[0] != 0 -> {
            requestPermissions(
                this,
                arrayOf(
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                ),
                45
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
        DatePickerDialog(
            this,

            calendar.get(1),
            calendar.get(2),
            calendar.get(5)
        ).show()
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
    this.batteryManager = systemService as BatteryManager
    val marker = BatteryLevelMarker(this, R.layout.marker_view)
    chart = this.findViewById(R.id.chart)
    chart.setDrawGridBackground(false)
    chart.setMarker(marker)
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
//        var xaxis:XAxis
        if (writePermission) {
            val parser = LogParser();
            val textView =findViewById<TextView>(R.id.currentInstanceTextView);
            val name = file.name;
            textView.setText((name, ".txt", "", false, 4, (Object) null), "-", " ", false, 4, (Object) null));
            try {
                val rawDataList = parser.read(file);
                calculateStats(rawDataList);
                val entries = ArrayList<Entry>();
                if (rawDataList.isNotEmpty()) {
                    val it = rawDataList . iterator ();
                    var averageLevel = 0;
                    while (!it.hasNext().equals(0)) {
                        val i = it.next();
                        averageLevel += i.level
                        entries.add(Entry(((i.hh * 60) + i.mm).toFloat(),  i.level.toFloat()));
                    }
                    try {
                        val size = averageLevel / rawDataList.size
                    } catch ( e:ArithmeticException) {
                        e.printStackTrace()
                    }
                    val textView2 = findViewById<TextView>(R.id.batteryLevel);
                    textView2.text = "${rawDataList.last().level}%"
                    val dataSet = LineDataSet(entries, "Battery level");
                    dataSet.lineWidth = 1.0f;
                    dataSet.setDrawValues(false);
                    dataSet.setDrawCircles(false);
                    dataSet.setDrawFilled(true);
                    dataSet.fillColor = Color.parseColor(resources.getString(R.color.colorFillGraph));
                    dataSet.color = Color.parseColor(resources.getString(R.color.colorGraphLine));
                    dataSet.mode = LineDataSet.Mode.LINEAR;
                    this.lineData = LineData(dataSet);
                    val lineChart:LineChart = this.chart;
                    val xAxis:XAxis
                    val yAxis:YAxis = lineChart.axisLeft
                    yAxis.granularity = 10.0f;
                    yAxis.gridColor = Color.parseColor(resources.getString(R.color.colorGraphGrid));
//                    val lineChart2 = this.chart;
                    val yAxisRight = chart.axisRight
                    if (yAxisRight != null) {
                        yAxisRight.isEnabled = false;
//                        val lineChart3 = this.chart;
                        xAxis = chart.xAxis
                        xAxis?.setDrawLabels(false)
//                        val lineChart4 = this.chart;
                        chart.data = this.lineData
//                        val lineChart5 = this.chart;
//                        if (!((xAxis = chart.getXAxis()) == null)) {
                        xAxis.setDrawGridLines(false);
//                        }
//                        val lineChart6 = this.chart;
                        chart.invalidate();
                    }
                }
            } catch (e2: IOException) {
                e2.printStackTrace();
                error( this, "MainActivity.drawChart() \ncannot read from " + file.getName().toString(), 0)
            } catch (e3: NoSuchElementException) {
                e3.printStackTrace();
                val textView3 =findViewById<TextView>(R.id.batteryVoltage);
                textView3.setText("not calculated yet");
                val textView4 = findViewById<TextView>(R.id.batteryLevel);
                textView4.setText("not calculated yet");
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
fun onClick(it: View?) {
    val button: Button =findViewById(R.id.watchCurrentData)
    button.visibility = View.VISIBLE
    watchingHistory=false
    drawChart()
}
fun onReceive(context: Context?, intent: Intent?) {
    if (!watchingHistory) {
        drawChart()
    }
}
fun onDateSet(
    `$noName_0`: DatePicker?,
    year1: Int,
    monthOfYear: Int,
    dayOfMonth: Int
) {
    val monthNew: String = monthList[monthOfYear]
    var dayNew = dayOfMonth.toString()
    if (dayOfMonth.toString().length == 1) {
        val sb = java.lang.StringBuilder()
        sb.append('0')
        sb.append(dayOfMonth)
        dayNew = sb.toString()
    } else {
        val i = dayOfMonth
    }
    val filesDir: File = this.filesDir
    val file =
        File(filesDir, "$dayNew-$monthNew-$year1.txt")
    if (file.exists()) {
        this.drawChart(file)
        watchingHistory=true
        val button: Button =
            this.findViewById(R.id.watchCurrentData)
        button.setVisibility(View.VISIBLE)
    }
warn(this,"No entry available",0)
    watchingHistory=false
}

fun onTick(p0: Long) {
    val textView =findViewById<TextView>(R.id.batteryVoltage)
    val sb = java.lang.StringBuilder()
    val `access$getBatteryStatusIntent$p`: Intent? = this.batteryStatusIntent
    var num: Int? = null
    sb.append(
            Integer.valueOf(
                `access$getBatteryStatusIntent$p`.getIntExtra("voltage", -1)
            )
    )
    sb.append("V")
    textView.text = sb.toString()
    val batteryManager: BatteryManager? = this.batteryManager
    num = Integer.valueOf(batteryManager?.getIntProperty(2).toString())
    val tmp = num
    if (tmp == null) {
        Intrinsics.throwNpe()
    }
    val tmp2 = Integer.valueOf(-tmp!!.toInt() / 1000)
    val textView2 =this.findViewById<TextView>(R.id.batteryCurrent)
    textView2.text = "$tmp2 mA"
}

private fun requestPermissions() {
    requestPermissions(
        this,
        arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        ),
        45
    )
}

fun timeTillFull(rawData: ArrayList<StatDataModel>): String {
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