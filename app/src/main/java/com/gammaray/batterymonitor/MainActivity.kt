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
        lateinit var instance: MainActivity
        var writePermission: Boolean=false
        var isCharging=false
        fun errorHandler(site: String?, error: String) {
            Log.e(site, error)
            Toast.makeText(instance, error, Toast.LENGTH_SHORT).show()
        }

        fun log(message: String, tag: String = "Testing") {
            Log.e(tag, message)
        }

        fun error(message: String, length: Int = Toasty.LENGTH_SHORT) {
            Log.e("ERROR", message)
            Toasty.error(instance, message, length).show()
        }

        fun warn(message: String, length: Int = Toasty.LENGTH_SHORT) {
            Intrinsics.checkParameterIsNotNull(message, "message")
            Log.e("WARNING", message)
            Toasty.warning(instance, message, length).show()
        }

        fun success(/*instance: Context,*/ message: String, length: Int = Toasty.LENGTH_SHORT) {
            Log.e("SUCCESS", message)
            Toasty.success(instance, message, length).show()
        }
    }
    init {
        instance = this
    }
    private val stats=Stats()
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
    private var watchingHistory = false
//    private var writePermission = false
    private lateinit var batteryManager: BatteryManager
    private var batteryStatus: Intent?=null
    private var status=0
    private val broadcastReceiver=object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!watchingHistory)
                drawChart()
        }
    }
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
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
        Handler(Looper.getMainLooper()).postDelayed(runnable, 1000)
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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permission: Array<String>,
        grantResults: IntArray
    ) = when {
        requestCode != READ_REQUEST_CODE -> super.onRequestPermissionsResult(
            requestCode,
            permission,
            grantResults
        )
        grantResults[0] != 0 -> requestPermissions()
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
                    sb.append(dayOfMonth)

                val filesDir: File = filesDir
                val file = File(filesDir, "$sb-$monthNew-${year1 % 100}.txt")
                watchingHistory = false
                if (fileProviderService.currentFile(this).name != file.name) {
                    if (file.exists()) {
                        watchingHistory = true
                        drawChart(file)
                        watchCurrentData.visibility = View.VISIBLE
                    } else
                        warn(/*this, */"No data present")

                }
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
        batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
        Handler(Looper.getMainLooper()).postDelayed(runnable, 1000)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        chart.setDrawGridBackground(false)
        chart.marker = BatteryLevelMarker(this, R.layout.marker_view)
        val yAxis:YAxis = chart.axisLeft
        yAxis.granularity = 10.0f
        yAxis.setDrawGridLines(true)
        yAxis.gridColor = ContextCompat.getColor(this, R.color.colorGraphGrid)
        val yAxisRight = chart.axisRight
        yAxisRight.isEnabled = false
        val xAxis = chart.xAxis
        xAxis.setDrawLabels(false)
        xAxis.setDrawGridLines(false)

        if (checkPermissions()) {
            startService(Intent(this, BatteryMonitorService::class.java))
            writePermission = true
            if (!watchingHistory)
                drawChart()
            return
        }
        error( /*this,*/ "give storage access")
        requestPermissions()
    }
    private fun drawChart(file: File = fileProviderService.currentFile(this)) {
        if (writePermission) {
            val parser = LogParser()
            if(watchingHistory) {
                val name = file.name.replace(".txt", "", true)
                currentInstanceTextView.text = name
//                statsPanelLayout.visibility=View.GONE
            }
            else{
                sb.clear()
                sb.append("Today")
                currentInstanceTextView.text = sb
                statsPanelLayout.visibility=View.VISIBLE
            }

            try {
                val rawDataList = ArrayList<Data>()
                val temp = parser.read(file)
                val n=temp.size
                for (i in 0 until n - 1)
                    if (temp[i].level != temp[i + 1].level)
                        rawDataList.add(temp[i])
                rawDataList.add(temp[n - 1])

                stats.updateStats(rawDataList)
                status=batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                timeLeftUntilFull.text = stats.timeTillFull(isCharging)
                timeLeftUntilEmpty.text = stats.timeTillEmpty(isCharging)
                lastChanged.text = stats.timeSinceLastChanged()
                batteryHealth.text = stats.batteryHealth()

                val entries = ArrayList<Entry>()
                if (rawDataList.isNotEmpty()) {
                    var averageLevel = 0
                    for(i in rawDataList){
                        averageLevel+=i.level
                        entries.add(Entry(((i.hh * 60) + i.mm).toFloat(), i.level.toFloat()))
                    }
                    try {
                        val size = averageLevel / rawDataList.size
//                            display average battery level
                    } catch (e: ArithmeticException) {
                        e.printStackTrace()
                    }
                    sb.clear()
                    sb.append(rawDataList.last().level)
                    sb.append("%")
                    batteryLevel.text = sb
                    val dataSet = LineDataSet(entries, "Battery level")
                    dataSet.init()
                    chart.data =LineData(dataSet)
                    chart.invalidate()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                error(/* this,*/ "MainActivity.drawChart() \ncannot read from " + file.name.toString())
            } catch (e: NoSuchElementException) {
                e.printStackTrace()
                sb.clear()
                sb.append("not calculated yet")
                batteryVoltage.text=sb
                batteryLevel.text=sb
                error( /*this,*/ "No Entry present in the system")
            }catch (e:ArrayIndexOutOfBoundsException){
                e.printStackTrace()
            }
        }
    }
    private fun LineDataSet.init(){
        this.lineWidth = 1.0f
        this.setDrawValues(false)
        this.setDrawCircles(false)
        this.setDrawFilled(true)
        this.fillColor =ContextCompat.getColor(this@MainActivity, R.color.colorFillGraph)
        this.color =ContextCompat.getColor(this@MainActivity, R.color.colorGraphLine)
        this.mode = LineDataSet.Mode.LINEAR
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