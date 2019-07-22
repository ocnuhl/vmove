package com.github.ocnuhl.vmove

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.amap.api.maps.MapView

class MainActivity : Activity() {
    private val TAG = "MainActivity"
    private lateinit var mMapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMapView = findViewById(R.id.map)
        mMapView.onCreate(savedInstanceState)
        initMap()
    }

    private fun initMap() {
        Log.d(TAG, "initMap")
        val aMap = mMapView.map
        aMap.setOnMapClickListener { latLng ->
            Log.d(TAG, "click: ${latLng.latitude}, ${latLng.longitude}")
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView.onSaveInstanceState(outState)
    }
}
