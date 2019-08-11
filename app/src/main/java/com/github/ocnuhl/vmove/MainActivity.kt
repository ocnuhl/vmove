package com.github.ocnuhl.vmove

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.utils.overlay.MovingPointOverlay
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : Activity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var mFab: FloatingActionButton
    private lateinit var mMapView: MapView
    private lateinit var mMarker: MovingPointOverlay
    private lateinit var mThread: ReportPositionThread
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                LocalService.ACTION_SERVICE_STATE_CHANGED -> {
                    val isRunning = intent.getBooleanExtra(LocalService.SERVICE_STATE, false)
                    val resId = if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    mFab.setImageResource(resId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupFab()
        setupMap(savedInstanceState)
        setupReceiver()
    }

    private fun setupFab() {
        mFab = findViewById<FloatingActionButton>(R.id.fab).also { fab ->
            val resId =
                if (LocalService.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            fab.setImageResource(resId)
            fab.setOnClickListener {
                val intent = Intent(this, LocalService::class.java)
                if (LocalService.isRunning) {
                    stopService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        val mapOptions = AMapOptions()
        val lastPos = loadLastPos()
        mapOptions.camera(CameraPosition(lastPos, 16f, 0f, 0f))
        mMapView = MapView(this, mapOptions)
        mMapView.onCreate(savedInstanceState)
        findViewById<ViewGroup>(R.id.container).addView(mMapView)

        val aMap = mMapView.map
        aMap.uiSettings.isZoomControlsEnabled = false

        MarkerOptions().also {
            it.position(lastPos).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_navigation))
            val marker = aMap.addMarker(it)
            mMarker = MovingPointOverlay(aMap, marker)
        }

        aMap.setOnMapClickListener { latLng ->
            mMarker.apply {
                stopMove()
                setPoints(listOf<LatLng>(mMarker.position, latLng))
                val time = calcDistance(mMarker.position, latLng) / 80 * 3600
                setTotalDuration(time.roundToInt())
                startSmoothMove()
            }
        }
        aMap.setOnMapLongClickListener { latLng ->
            mMarker.apply {
                stopMove()
                position = latLng
            }
        }
        mThread = ReportPositionThread().apply { start() }
    }

    private fun calcDistance(pos1: LatLng, pos2: LatLng): Double {
        val avgLat = (pos1.latitude + pos2.latitude) * PI / 360
        val disLat = 6371 * cos(avgLat) * ((pos1.longitude - pos2.longitude) * PI / 180)
        val disLng = 6371 * ((pos1.latitude - pos2.latitude) * PI / 180)
        return sqrt(disLat * disLat + disLng * disLng)
    }

    private fun reportPosition(latLng: LatLng) {
        val intent = Intent(this, LocalService::class.java)
        intent.action = LocalService.ACTION_REPORT_POSITION
        intent.putExtra(LocalService.LAT, latLng.latitude)
        intent.putExtra(LocalService.LNG, latLng.longitude)
        startService(intent)
    }

    private fun setupReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(LocalService.ACTION_SERVICE_STATE_CHANGED)
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        saveLastPos()
        mThread.interrupt()
        mMarker.destroy()
        mMapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView.onSaveInstanceState(outState)
    }

    private fun saveLastPos() {
        val target = mMarker.position
        val editor = getSharedPreferences(TAG, MODE_PRIVATE).edit()
        editor.putDouble(LocalService.LAT, target.latitude)
        editor.putDouble(LocalService.LNG, target.longitude)
        editor.apply()
    }

    private fun loadLastPos(): LatLng {
        val pref = getSharedPreferences(TAG, MODE_PRIVATE)
        val lastLat = pref.getDouble(LocalService.LAT, 39.904989)
        val lastLng = pref.getDouble(LocalService.LNG, 116.405285);
        return LatLng(lastLat, lastLng)
    }

    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

    inner class ReportPositionThread : Thread("ReportPositionThread") {
        override fun run() {
            Log.i(TAG, "Start reporting position")
            try {
                while (true) {
                    if (LocalService.isRunning) {
                        reportPosition(mMarker.position)
                    }
                    sleep(1000)
                }
            } catch (e: InterruptedException) {
                Log.i(TAG, "Stop reporting position")
            }
        }
    }
}
