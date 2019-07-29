package com.github.ocnuhl.vmove

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class LocalService : Service() {
    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocalService = this@LocalService
    }
}