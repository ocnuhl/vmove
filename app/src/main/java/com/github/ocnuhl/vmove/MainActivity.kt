package com.github.ocnuhl.vmove

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.amap.api.maps.AMapOptions
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng

class MainActivity : Activity() {
    private val TAG = "MainActivity"
    private val KEY_LAST_LAT = "KEY_LAST_LAT"
    private val KEY_LAST_LNG = "KEY_LAST_LNG"
    private lateinit var mMapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapOptions = AMapOptions()
        mapOptions.camera(CameraPosition(loadLastPos(), 16f, 0f, 0f))
        mMapView = MapView(this, mapOptions)
        mMapView.onCreate(savedInstanceState)

        findViewById<ViewGroup>(R.id.container).addView(mMapView)
        initMap()
    }

    private fun initMap() {
        val aMap = mMapView.map
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.setOnMapClickListener { latLng ->
            Log.d(TAG, "click: ${latLng.latitude}, ${latLng.longitude}")
        }
        aMap.setOnMapLongClickListener { latLng ->
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
