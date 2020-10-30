package com.gammaray.batterymonitor

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
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

        fun logger(message: String, tag: String="Testing") {
            Log.e(tag, message)
        }

        fun error(instance: Context, message: String, length: Int=Toasty.LENGTH_SHORT) {
            Log.e("ERROR", message)
            Toasty.error(instance, message, length).show()
        }

        fun warn(instance: Context, message: String, length: Int=Toasty.LENGTH_SHORT) {
            Intrinsics.checkParameterIsNotNull(message, "message")
            Log.e("WARNING", message)
            Toasty.warning(instance, message, length).show()
        }

        fun success(instance: Context, message: String, length: Int=Toasty.LENGTH_SHORT) {
            Log.e("SUCCESS", message)
            Toasty.success(instance, message, length).show()
        }
    }
    init {
        instance = this
    }
    private val monthList = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    var watchingHistory = false
    private var writePermission = false
    private lateinit var batteryManager: BatteryManager
    private var batteryStatus: Intent?=null
    private val broadcastReceiver=object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!watchingHistory)
                drawChart()
        }
    }
    private lateinit var lineData: LineData
    private val fileProviderService = FileProviderService()
    //private val quickUpdate=//
    private val sb = StringBuilder()
    private val runnable= object:Runnable {
        override fun run() {
            sb.clear()
            sb.append(batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1))
            sb.append("mV")
            batteryVoltage.text = sb.toString()
            val num: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val tmp: Int = -num / 1000
            sb.clear()
            sb.append(tmp)
            sb.append("mA")
            batteryCurrent.text = sb
            Handler(Looper.getMainLooper()).postDelayed(this, 1000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //    quickUpdate.start()

        initialize()
    }
    override fun onResume() {
        //    quickUpdate.start()
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed(runnable,1000)
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
        if (!watchingHistory)
            drawChart()
    }
    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }
    override fun onPause() {
        //    this.quickUpdate.cancel()
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(runnable)
        super.onPause()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>,grantResults: IntArray) = when {
        requestCode != READ_REQUEST_CODE -> super.onRequestPermissionsResult(requestCode, permission, grantResults)
        grantResults[0] != 0 -> requestPermissions(
            this,
            arrayOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ),
            READ_REQUEST_CODE
        )
        else -> {
            finish()
            startActivity(intent)
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
                sb.clear()
                if (dayOfMonth.toString().length == 1) {
                    sb.append('0')
                    sb.append(dayOfMonth)
                } else
                    sb.append(dayOfMonth.toString())

                val filesDir: File = filesDir
                val file = File(filesDir, "$sb-$monthNew-$year1.txt")
                watchingHistory=false
                if (file.exists() && fileProviderService.currentFile(this).name !=file.name) {
                    watchingHistory=true
                    drawChart(file)
                    watchCurrentData.visibility = View.VISIBLE
                }
                else
                    warn(this,"No entry available",0)

            }, year, month, day)
            dpd.datePicker.maxDate=System.currentTimeMillis()
            dpd.show()
        }
        return true
    }
    fun viewCurrent(it: View?) {
        watchCurrentData.visibility = View.GONE
        watchingHistory=false
        drawChart()
    }
    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    private fun initialize() {
        Handler(Looper.getMainLooper()).postDelayed(runnable,1000)
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
        batteryStatus = registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        chart.setDrawGridBackground(false)
        chart.marker = BatteryLevelMarker(this, R.layout.marker_view) //marker
        if (checkPermissions()) {
            startService(Intent(this, BatteryMonitorService::class.java))
            writePermission = true
            if (!watchingHistory)
                drawChart()
            return
        }
        error( this, "give storage access",0)
        requestPermissions()
    }
    private fun drawChart(file:File = fileProviderService.currentFile(this)) {
        logger(writePermission.toString())
        if (writePermission) {
            val parser = LogParser()
            if(watchingHistory) {
                val name = file.name.replace(".txt", "", true)
                currentInstanceTextView.text = name
            }
            else
                currentInstanceTextView.text = "Today"

            try {
                val rawDataList = parser.read(file)
                val stats=Stats()
//                stats.calculateStats(rawDataList)
                val entries = ArrayList<Entry>()
                if (rawDataList.isNotEmpty()) {
                    var averageLevel = 0
                    for(i in rawDataList){
                        averageLevel+=i.level
                        entries.add(Entry(((i.hh * 60) + i.mm).toFloat(),  i.level.toFloat()))
                    }
                    try {
                        val size = averageLevel / rawDataList.size
//                            display average battery level
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
                    dataSet.fillColor =ContextCompat.getColor(this,R.color.colorFillGraph)
                    dataSet.color =ContextCompat.getColor(this,R.color.colorGraphLine)
                    dataSet.mode = LineDataSet.Mode.LINEAR
                    lineData = LineData(dataSet)
                    val yAxis:YAxis = chart.axisLeft
                    yAxis.granularity = 10.0f
                    yAxis.setDrawGridLines(true)
                    yAxis.gridColor = ContextCompat.getColor(this,R.color.colorGraphGrid)
                    val yAxisRight = chart.axisRight
                    yAxisRight.isEnabled = false
                    val xAxis = chart.xAxis
                    xAxis.setDrawLabels(false)
                    chart.data = lineData
                    xAxis.setDrawGridLines(false)
                    chart.invalidate()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                error( this, "MainActivity.drawChart() \ncannot read from " + file.name.toString(), 0)
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                sb.clear()
                sb.append("not calculated yet")
                batteryVoltage.text=sb
                batteryLevel.text=sb
                error( this, "No Entry present in the system", 0)
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
}