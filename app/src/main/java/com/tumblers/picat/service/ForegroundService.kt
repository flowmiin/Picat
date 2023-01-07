package com.tumblers.picat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import com.tumblers.picat.R
import kotlin.concurrent.thread

class ForegroundService : Service() {

    val CHANNEL_ID = "PICAT5"
    val NOTI_ID = 5

    fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "FOREGROUND", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Picat이 백그라운드 실행중입니다.")
            .setSmallIcon(R.mipmap.picat_app_icn)
            .build()

        startForeground(NOTI_ID, notification)

        runBackground()

        return super.onStartCommand(intent, flags, startId)
    }

    fun runBackground() {
        thread(start = true) {
            val intent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            println("intent = ${intent}")
            println("intent data = ${intent.data}")
            if(intent.data != null) {
                var bitmap: Bitmap = intent.data as Bitmap
                println("bitmap : ${bitmap}")
            }
        }
    }



    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }
}