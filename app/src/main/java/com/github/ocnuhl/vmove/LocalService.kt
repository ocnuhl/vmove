package com.github.ocnuhl.vmove

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.amap.api.maps.model.LatLng

const val TAG = "LocalService"

class LocalService : Service() {
    companion object {
        var isRunning = false
    }
    private val mBinder = LocalBinder()
    private lateinit var currentPos: LatLng
    private lateinit var destination: LatLng

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        isRunning = false
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocalService = this@LocalService
    }

    fun setCurrentPos(latLng: LatLng) {
        currentPos = latLng
        destination = latLng
    }

    fun setDestination(latLng: LatLng) {
        destination = latLng
    }
}