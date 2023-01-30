package com.tumblers.picat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tumblers.picat.R
import com.tumblers.picat.SharePictureActivity
import com.tumblers.picat.SocketApplication
import com.tumblers.picat.dataclass.ImageResponseData
import com.tumblers.picat.dataclass.RequestInterface
import io.socket.client.Socket
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File


class ForegroundService : Service() {

    var startImage : Uri? = null
    var startImageDate : Long? = null
    var currentImageDate : Long? = null

    lateinit var mSocket: Socket

    var myKakaoId: Long? = null


    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        createNotification()
        mThread!!.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 액티비티에서 서비스로 넘어 올 때 갤러리에서 가장 최근의 이미지를 저장
        // 나중에 새로운 이미지가 들어오면 비교해야한다.
        startImage = getTheLastImage()
        if (startImage != null) {
            startImageDate = getImageDateTaken(startImage)
            currentImageDate = startImageDate
        }
        myKakaoId = intent.getLongExtra("myKakaoId", -1)

        return super.onStartCommand(intent, flags, startId)
    }

    // 백그라운드에서 실행 할 작업
    private var mThread: Thread? = object : Thread("My Thread") {
        override fun run() {
            super.run()
            mSocket = SocketApplication.get()
            mSocket.connect()
            while(currentThread() != null) {
                try {
                    handlerNewPhotos()
                    sleep(1000)
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                    break
                }
            }
        }
    }

    // notificatoin 알람 설정
    private fun createNotification() {
        val channelId = "Picat_channel_1"
        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.mipmap.picat_app_icn)
        builder.setContentTitle("피캣 자동 업로드")
        builder.setContentText("Picat이 최신 사진을 자동 업로드 합니다.")
        val notificationIntent = Intent(this, SharePictureActivity::class.java)

        notificationIntent.action = Intent.ACTION_CAMERA_BUTTON

        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "자동 업로드 채널", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        notificationManager.notify(NOTI_ID, builder.build())
        val notification = builder.build()
        startForeground(NOTI_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mThread != null) {
            mThread!!.interrupt()
            mThread = null
            mSocket.disconnect()
            mSocket.close()
        }
        Log.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "MyServiceTag"
        private const val NOTI_ID = 5
    }

    fun handlerNewPhotos() {
        // 새로운 이미지 얻기
        val newImage = getTheLastImage()
        val newImageDate = getImageDateTaken(newImage)


        // 새로운 이미지 날짜와 기존 이미지 날짜를 비교
        if ((currentImageDate != null) && (newImageDate != null) && (newImageDate!! > currentImageDate!!)) {
            currentImageDate = newImageDate
            var emitBody : MutableList<MultipartBody.Part>? = mutableListOf()
            var file = File(getAbsolutePath(newImage, this))
            var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
            var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            emitBody?.add(body)
            apiRequest(emitBody, 1)
        }

        // 갤러리에 이미지가 없는 상태에서 새로운 이미지가 들어왔을떄
//        else if ((currentImageDate == null) && (newImageDate != null)) {
//            startImage = newImage!!
//            startImageDate = getImageDateTaken(startImage)
//            currentImageDate = startImageDate
//
//            var emitBody : MutableList<MultipartBody.Part>? = mutableListOf()
//            var file = File(getAbsolutePath(newImage, this))
//            var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
//            var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
//            emitBody?.add(body)
//            apiRequest(emitBody, 1)
//        }

    }

    // 사진의 절대 경로 가져오기
    fun getAbsolutePath(path: Uri?, context : Context): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        var index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        var result = c?.getString(index!!)

        return result!!
    }

    // 서버로 post 요청보내기
    private fun apiRequest(image_multipart: MutableList<MultipartBody.Part>?, img_cnt: Int) {
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(resources.getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postImg(myKakaoId!!, image_multipart!!, img_cnt, myKakaoId!!).enqueue(object : Callback<ImageResponseData> {

            override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                println("이미지 업로드 ${response.isSuccessful}")
                if (response.isSuccessful){
                    val jsonArray = JSONArray()

                    for (i in 0..response.body()?.img_cnt!! - 1) {
                        val jsonObject = JSONObject()
                        jsonObject.put("url", response.body()?.url?.toList()?.get(i))
                        jsonArray.put(jsonObject)
                    }

                    val jsonObject = JSONObject()
                    jsonObject.put("img_list", jsonArray)
                    jsonObject.put("img_cnt", response.body()?.img_cnt)
                    jsonObject.put("id", myKakaoId)

                    mSocket.emit("image", jsonObject)
                }
            }

            override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                println("이미지 업로드 실패")
            }
        })
    }

    private val projection = arrayOf(
        MediaStore.Images.ImageColumns._ID,
        MediaStore.Images.Media._ID,
        MediaStore.Images.ImageColumns.DATE_ADDED,
        MediaStore.Images.ImageColumns.MIME_TYPE
    )

    // 갤러리의 사진 개수
    fun getAmountImage(): Int {
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        val amountImage = cursor!!.count
        cursor.close()
        return amountImage
    }

    // 갤러리의 가장 최근 이미지
    fun getTheLastImage(): Uri? {
        var imageUri: Uri? = null
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        if(cursor!!.moveToFirst()) {
            val columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val imageId = cursor.getLong(columnIndexId)
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageId)
        }
        getImageDateTaken(imageUri)

        cursor.close()
        return imageUri
    }

    // 갤러리의 가장 오래된 이미지
    fun getTheOldImage(): Uri? {
        var imageUri: Uri? = null
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        if(cursor!!.moveToLast()) {
            val columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val imageId = cursor.getLong(columnIndexId)
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageId)
        }
        getImageDateTaken(imageUri)

        cursor.close()
        return imageUri
    }

    // 해당 uri의 날짜 정보 가져오기
    fun getImageDateTaken(imageUri: Uri?): Long? {
        val path = getPath(imageUri)
        val file = path?.let { File(it) }

        file?.let {
            if (file.exists()) {
                return file.lastModified()
            }
        }
        return null
    }

    // 이미지의 절대경로 가져오기
    private fun getPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = uri?.let {
            contentResolver.query(it, projection, null, null, null)
        }?: return null
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        if (cursor.moveToFirst()) {
            val s = cursor.getString(columnIndex)
            cursor.close()
            return s
        }
        cursor.close()
        return null
    }
}