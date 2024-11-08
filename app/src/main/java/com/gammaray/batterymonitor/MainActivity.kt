package com.gammaray.batterymonitor

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.*
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

        fun warn(message: String, length: Int = Toasty.LENGTH_SHORT) {
            Intrinsics.checkParameterIsNotNull(message, "message")
            Log.e("WARNING", message)
            Toasty.warning(instance, message, length).show()
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
    private lateinit var batteryManager: BatteryManager
    private var batteryStatus: Intent?=null
    private var status=0
    private val delay=1000L
    private val broadcastReceiver=object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!watchingHistory)
                drawChart()
        }
    }
    private val fileProviderService = FileProviderService()
    private val sb = StringBuilder()
    //a runnable for updating stats in notification
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
            Handler(Looper.getMainLooper()).postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))
        Handler(Looper.getMainLooper()).postDelayed(runnable, delay)
        if (!watchingHistory)
            drawChart()
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onPause() {
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
        when (itemId) {
            R.id.exit -> {
                finish()
                return true
            }
            R.id.source -> {
                val srcUrl="https://github.com/gammaraylab/Battery-Monitor"
                val intent= Intent(Intent.ACTION_VIEW)
                intent.data= Uri.parse(srcUrl)
                startActivity(intent)
                return true
            }
            //display option for watching history
            R.id.history -> {
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
                return true
            }
        }
        return false
    }
    //display today's data (clickable only if the user is watching the battery history)
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
        //init the notification
        batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(broadcastReceiver, IntentFilter(BatteryMonitorService.UPDATE_FLAG))

        Handler(Looper.getMainLooper()).postDelayed(runnable, delay)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        //init the chart, define how the chart will look
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                this.startForegroundService(Intent(this, BatteryMonitorService::class.java))
            else
                startService(Intent(this, BatteryMonitorService::class.java))

            writePermission = true
            if (!watchingHistory)
                drawChart()
            return
        }
        requestPermissions()
    }

    //plot the actual chart
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
                // read data from text file into ArrayList
                val rawDataList = ArrayList<Data>()
                val temp = parser.read(file)
                val n=temp.size
                for (i in 0 until n - 1)
                    if (temp[i].level != temp[i + 1].level)
                        rawDataList.add(temp[i])
                rawDataList.add(temp[n - 1])

                stats.updateStats(rawDataList)  //update the battery stats

                status=batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                //display the stats to user
                timeLeftUntilFull.text = stats.timeTillFull(isCharging)
                timeLeftUntilEmpty.text = stats.timeTillEmpty(isCharging)
                lastChanged.text = stats.timeSinceLastChanged()

                val entries = ArrayList<Entry>()
                if (rawDataList.isNotEmpty()) {
                    var averageLevel = 0
                    for(i in rawDataList){  //make an entry set for plotting the graph
                        averageLevel+=i.level
                        entries.add(Entry(((i.hh * 60) + i.mm).toFloat(), i.level.toFloat()))
                    }

                    sb.clear()
                    sb.append(rawDataList.last().level)
                    sb.append("%")
                    batteryLevel.text = sb
                    val dataSet = LineDataSet(entries, "Battery level")
                    dataSet.init()  //init the graph parameters
                    chart.data =LineData(dataSet)
                    chart.invalidate()  //update the previous chart with current data
                }
            }
            catch (e: NoSuchElementException) {
                sb.clear()
                sb.append("not calculated yet")
                batteryVoltage.text=sb
                batteryLevel.text=sb
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    //initialize the Linedataset for graph plotting
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