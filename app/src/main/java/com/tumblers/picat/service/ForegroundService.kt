package com.tumblers.picat.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        throw java.lang.UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) : Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private var mThread: Thread? = object : Thread("My Thread") {
        override fun run() {
            super.run()
            for (i in 0..99) {
                try {
                    sleep(1000)
                } catch(e: InterruptedException) {
                    currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun createNotification() {
        val builder = NotificationCompat.Builder(this, "default")
    }
}