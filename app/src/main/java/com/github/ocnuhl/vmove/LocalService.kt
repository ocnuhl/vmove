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
import kotlin.math.*

class LocalService : Service() {
    companion object {
        @Volatile
        var isRunning = false
        const val TAG = "LocalService"
        const val ACTION_SERVICE_STATE_CHANGED = "action.SERVICE_STATE_CHANGED"
        const val ACTION_REPORT_POSITION = "action.REPORT_POSITION"
        const val SERVICE_STATE = "SERVICE_STATE"
        const val LAT = "LAT"
        const val LNG = "LNG"
    }

    @Volatile
    private var mPosition: LatLng? = null
    private lateinit var mThread: ServiceThread

    override fun onCreate() {
        super.onCreate()
        mThread = ServiceThread().apply { start() }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_REPORT_POSITION -> {
                val lat = intent.getDoubleExtra(LAT, 0.0)
                val lng = intent.getDoubleExtra(LNG, 0.0)
                mPosition = LatLng(lat, lng)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mThread.interrupt()
    }

    private fun setServiceState(state: Boolean) {
        isRunning = state
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED)
        intent.putExtra(SERVICE_STATE, state)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private inner class ServiceThread : Thread(TAG) {
        private val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        override fun run() {
            try {
                startMockLocation()
                setServiceState(true)
                while (true) {
                    mPosition?.let { mockLocation(convertLocation(it)) }
                    sleep(1000)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Please enable mock location")
                stopSelf()
            } catch (e: InterruptedException) {
                Log.i(TAG, "Service stopped")
            } catch (e: Exception) {
                Log.e(TAG, "error: ${e.message}")
                stopSelf()
            }
            setServiceState(false)
            stopMockLocation()
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
            try {
                lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
                lm.removeTestProvider(LocationManager.GPS_PROVIDER)
                Log.i(TAG, "Mock provider disabled")
            } catch (e: Exception) {
                Log.e(TAG, "error: ${e.message}")
            }
        }

        private fun convertLocation(latLng: LatLng): LatLng {
            val x = latLng.longitude - 105
            val y = latLng.latitude - 35
            val xPI = x * PI
            val yPI = y * PI
            var dLat = 20 * (sin(6 * xPI) + sin(2 * xPI))
            var dLng = dLat
            dLat += 20 * sin(yPI) + 40 * sin(yPI / 3)
            dLng += 20 * sin(xPI) + 40 * sin(xPI / 3)
            dLat += 160 * sin(yPI / 12) + 320 * sin(yPI / 30)
            dLng += 150 * sin(xPI / 12) + 300 * sin(xPI / 30)
            dLat *= 2.0 / 3.0
            dLng *= 2.0 / 3.0
            val a = x * y * 0.1
            val b = sqrt(abs(x))
            dLat += 2 * x + 3 * y + 0.2 * y * y + a + 0.2 * b - 100
            dLng += x + 2 * y + 0.1 * x * x + a + 0.1 * b + 300
            val c = 0.00669342162296594323
            val d = latLng.latitude / 180 * PI
            var e = sin(d)
            e = 1 - c * e * e
            val f = sqrt(e)
            val g = 6378137.0
            dLat = dLat * 180 / ((g * (1 - c)) / (e * f) * PI)
            dLng = dLng * 180 / (g / f * cos(d) * PI)
            return LatLng(latLng.latitude - dLat, latLng.longitude - dLng)
        }
    }
}