package com.github.ocnuhl.vmove

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.ViewGroup
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : Activity() {
    private val TAG = "MainActivity"
    private val KEY_LAST_LAT = "KEY_LAST_LAT"
    private val KEY_LAST_LNG = "KEY_LAST_LNG"
    private lateinit var mMapView: MapView
    private lateinit var mFab: FloatingActionButton
    private lateinit var mService: LocalService
    private var mBound = false

    private val connection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as LocalService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(className: ComponentName?) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapOptions = AMapOptions()
        mapOptions.camera(CameraPosition(loadLastPos(), 16f, 0f, 0f))
        mMapView = MapView(this, mapOptions)
        mMapView.onCreate(savedInstanceState)
        findViewById<ViewGroup>(R.id.container).addView(mMapView)
        initMap()

        mFab = findViewById(R.id.fab)
        setupFab()
    }

    private fun setupFab() {
        if (LocalService.isRunning)
            mFab.setImageResource(android.R.drawable.ic_media_pause)
        else
            mFab.setImageResource(android.R.drawable.ic_media_play)

        mFab.setOnClickListener {
            val intent = Intent(this, LocalService::class.java)
            if (LocalService.isRunning) {
                stopService(intent)
                LocalService.isRunning = false
                mFab.setImageResource(android.R.drawable.ic_media_play)
            } else {
                startService(intent)
                mFab.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, LocalService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    private fun initMap() {
        val aMap = mMapView.map
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.setOnMapClickListener { latLng ->
            Log.d(TAG, "click: ${latLng.latitude}, ${latLng.longitude}")
            if (mBound) {
                mService.setDestination(latLng)
            }
        }
        aMap.setOnMapLongClickListener { latLng ->
            if (mBound) {
                mService.setCurrentPos(latLng)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

    private fun loadLastPos() : LatLng {
        val pref = getSharedPreferences(TAG, MODE_PRIVATE)
        val lastLat = pref.getDouble(KEY_LAST_LAT, 39.904989)
        val lastLng = pref.getDouble(KEY_LAST_LNG,116.405285);
        return LatLng(lastLat, lastLng)
    }

    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))
}
