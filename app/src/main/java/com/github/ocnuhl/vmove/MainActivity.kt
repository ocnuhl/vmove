package com.github.ocnuhl.vmove

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : Activity() {
    companion object {
        const val TAG = "MainActivity"
        const val KEY_LAST_LAT = "KEY_LAST_LAT"
        const val KEY_LAST_LNG = "KEY_LAST_LNG"
    }

    private lateinit var mFab: FloatingActionButton
    private lateinit var mMapView: MapView
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
        mapOptions.camera(CameraPosition(loadLastPos(), 16f, 0f, 0f))
        mMapView = MapView(this, mapOptions)
        mMapView.onCreate(savedInstanceState)
        findViewById<ViewGroup>(R.id.container).addView(mMapView)

        val aMap = mMapView.map
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.setOnMapClickListener { latLng ->
            Log.d(TAG, "click: ${latLng.latitude}, ${latLng.longitude}")
        }
        aMap.setOnMapLongClickListener { latLng ->
        }
    }

    private fun setupReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(LocalService.ACTION_SERVICE_STATE_CHANGED)
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
        mMapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView.onPause()
        saveLastPos()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView.onSaveInstanceState(outState)
    }

    private fun saveLastPos() {
        val target = mMapView.map.cameraPosition.target
        val editor = getSharedPreferences(TAG, MODE_PRIVATE).edit()
        editor.putDouble(KEY_LAST_LAT, target.latitude)
        editor.putDouble(KEY_LAST_LNG, target.longitude)
        editor.apply()
    }

    private fun loadLastPos(): LatLng {
        val pref = getSharedPreferences(TAG, MODE_PRIVATE)
        val lastLat = pref.getDouble(KEY_LAST_LAT, 39.904989)
        val lastLng = pref.getDouble(KEY_LAST_LNG, 116.405285);
        return LatLng(lastLat, lastLng)
    }

    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))
}
