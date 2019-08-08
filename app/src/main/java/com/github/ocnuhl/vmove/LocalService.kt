package com.github.ocnuhl.vmove

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amap.api.maps.model.LatLng

class LocalService : Service() {
    companion object {
        @Volatile
        var isRunning = false
        const val TAG = "LocalService"
        const val ACTION_SERVICE_STATE_CHANGED = "action.SERVICE_STATE_CHANGED"
        const val ACTION_SET_CURRENT_POS = "action.SET_CURRENT_POS"
        const val ACTION_SET_DESTINATION = "action.SET_DESTINATION"
        const val SERVICE_STATE = "SERVICE_STATE"
        const val LAT = "LAT"
        const val LNG = "LNG"
    }

    @Volatile
    private var currentPos: LatLng? = null
    @Volatile
    private var destination: LatLng? = null
    private lateinit var mThread: ServiceThread

    override fun onCreate() {
        super.onCreate()
        mThread = ServiceThread().also { it.start() }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val lat = intent.getDoubleExtra(LAT, 0.0)
        val lng = intent.getDoubleExtra(LNG, 0.0)
        when (intent.action) {
            ACTION_SET_CURRENT_POS -> {
                currentPos = LatLng(lat, lng)
                destination = currentPos
            }
            ACTION_SET_DESTINATION -> {
                destination = LatLng(lat, lng)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mThread.shouldQuit = true
    }

    private fun setServiceState(state: Boolean) {
        isRunning = state
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED)
        intent.putExtra(SERVICE_STATE, state)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private inner class ServiceThread : Thread(TAG) {
        @Volatile
        var shouldQuit = false
        private val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        override fun run() {
            setServiceState(true)
            try {
                startMockLocation()
                while (!shouldQuit) {
                    currentPos?.let { mockLocation(it) }
                    sleep(1000)
                }
                stopMockLocation()
            } catch (e: SecurityException) {
                Log.e(TAG, "Please enable mock location")
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "error: ${e.message}")
                stopSelf()
            }
            setServiceState(false)
        }

        private fun startMockLocation() {
            try {
                lm.addTestProvider(
                    LocationManager.GPS_PROVIDER, false, false,
                    false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Mock provider already exists")
            }
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            Log.i(TAG, "Mock provider enabled")
        }

        private fun mockLocation(latLng: LatLng) {
            val location = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = latLng.latitude
                longitude = latLng.longitude
                accuracy = 1f
                altitude = 30.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
        }

        private fun stopMockLocation() {
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
            Log.i(TAG, "Mock provider disabled")
        }
    }
}