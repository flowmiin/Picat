package com.tumblers.picat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tumblers.picat.R
import com.tumblers.picat.SharePictureActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FirebaseService"

    // Token 생성
    override fun onNewToken(token: String) {
        // 토큰 값을 따로 저장
        val pref = this.getSharedPreferences("token", Context.MODE_PRIVATE)
        pref.edit().putString("token", token).apply()
        pref.edit().commit()
        println("내 토큰 : ${pref.getString("token", "nothing")}")
    }

    // 메시지 수신
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 포그라운드 상태에서 Notification을 받는 경우
        if(remoteMessage.data.isNotEmpty()) {
            println("From : ${remoteMessage!!.from}")
            sendNotification(remoteMessage)
        }
        else {
            println("메시지를 수신하지 못했습니다.")
        }
    }

    // FCM 메시지를 보낸다
    private fun sendNotification(remoteMessage: RemoteMessage) {
        /* 알람을 누르면 실행되는 액티비티를 설정 */
        val intent = Intent(this, SharePictureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // 액티비티 중복 생성 방지
        }

        for(key in remoteMessage.data.keys){
            intent.putExtra(key, remoteMessage.data.getValue(key))
        }

        val resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val channelId = "picat_channel_2"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.picat_app_icn)
            .setContentTitle("Picat 알림")
            .setContentText("방에 초대되었습니다.")
            .setAutoCancel(true) // 알림 클릭시 삭제 여부
            .setContentIntent(resultPendingIntent) // 알림 실행 시 intent

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "초대 채널", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        // 알림 생성
        notificationManager.notify(1, notificationBuilder.build())
    }

    // Token 가져오기
    fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            Log.d(TAG, "token=${it}")
        }
    }
}