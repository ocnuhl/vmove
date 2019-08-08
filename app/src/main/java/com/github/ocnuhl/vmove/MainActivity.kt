package com.github.ocnuhl.vmove

import android.app.Activity
import android.content.*
import android.os.Bundle
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
                    setPosition(LocalService.ACTION_SET_CURRENT_POS, loadLastPos())
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
            if (LocalService.isRunning) {
                setPosition(LocalService.ACTION_SET_DESTINATION, latLng)
            }
        }
        aMap.setOnMapLongClickListener { latLng ->
            if (LocalService.isRunning) {
                setPosition(LocalService.ACTION_SET_CURRENT_POS, latLng)
            }
        }
    }

    private fun setPosition(action: String, latLng: LatLng) {
        val intent = Intent(this, LocalService::class.java)
        intent.action = action
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
}
