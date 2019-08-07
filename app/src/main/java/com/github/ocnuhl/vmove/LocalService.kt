package com.github.ocnuhl.vmove

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amap.api.maps.model.LatLng

class LocalService : Service() {
    companion object {
        var isRunning = false
        const val TAG = "LocalService"
        const val ACTION_SERVICE_STATE_CHANGED = "action.SERVICE_STATE_CHANGED"
        const val SERVICE_STATE = "SERVICE_STATE"
    }

    private lateinit var currentPos: LatLng
    private lateinit var destination: LatLng
    private lateinit var mThread: ServiceThread

    override fun onCreate() {
        super.onCreate()
        mThread = ServiceThread().also { it.start() }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        notifyServiceState()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        notifyServiceState()
        mThread.shouldQuit = true
    }

    private fun notifyServiceState() {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED)
        intent.putExtra(SERVICE_STATE, isRunning)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private inner class ServiceThread : Thread(TAG) {
        @Volatile
        var shouldQuit = false

        override fun run() {
            Log.d(TAG, "ServiceThread start")
            while (!shouldQuit) {
                sleep(1000)
            }
            Log.d(TAG, "ServiceThread end")
        }
    }

    fun setCurrentPos(latLng: LatLng) {
        currentPos = latLng
        destination = latLng
    }

    fun setDestination(latLng: LatLng) {
        destination = latLng
    }
}