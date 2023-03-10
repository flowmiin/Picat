package com.tumblers.picat

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.friend.client.PickerClient
import com.kakao.sdk.friend.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.model.PickerOrientation
import com.kakao.sdk.friend.model.ViewAppearance
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import com.kakao.sdk.user.UserApiClient
import com.kakao.usermgmt.StringSet.tag
import com.tumblers.picat.adapter.*
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.databinding.ExitDialogBinding
import com.tumblers.picat.databinding.InviteCheckDialogBinding
import com.tumblers.picat.databinding.LoadingDialogBinding
import com.tumblers.picat.dataclass.*
import com.tumblers.picat.fragment.DownloadCompleteFragment
import com.tumblers.picat.service.ForegroundService
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest


class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter
    
    var mSocket: Socket? = null
    lateinit var bottomSheetDialog: BottomSheetDialog

    var joinFriendList: ArrayList<FriendData> = ArrayList()
    var imageDataList: ArrayList<ImageData> = ArrayList()
    var clearImageDataList: ArrayList<ImageData> = ArrayList()
    var blurImageDataList: ArrayList<ImageData> = ArrayList()
    var selectionIdList: HashSet<Int> = hashSetOf()

    // ?????? ?????????
    var myKakaoId : Long? = null
    var myNickname: String? = ""
    var myPicture: String? = ""
    var myEmail: String? = ""

    //???????????? ?????????
    var backKeyPressedTime: Long = 0

    // switch ?????? ??????
    lateinit var switch_pref : SharedPreferences
    lateinit var invite_pref : SharedPreferences

    private lateinit var progressDialog: AppCompatDialog
    private lateinit var exitDialog: AppCompatDialog
    private lateinit var inviteDialog: AppCompatDialog

    // ??????????????? ?????? ?????? ????????? ?????? X
    val mutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // socket ?????? ??????
        mSocket = SocketApplication.get()
        mSocket?.connect()
        mSocket?.on("image", onImage)
        mSocket?.on("join", onJoin)
        mSocket?.on("participate", onParticipate)
        mSocket?.on("exit", onExit)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                println("granted")
            }
            else {
                println("denied")
            }
        }


        // ?????? ?????? ?????? ???
        // ?????? ??? ????????? ???????????? ??????
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                val intent = Intent(this, LoginActivity::class.java)
                finish()
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            else if (tokenInfo != null) {
                //??? ?????? ??? ????????? ??????
                val permissions: Array<String> = arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this, permissions, PackageManager.PERMISSION_GRANTED)
                }
            }
        }

        // switch ?????? ?????? ?????? ??????
        switch_pref = getSharedPreferences("switch_pref", Context.MODE_PRIVATE)

        // ????????? ?????? ?????? ??? ?????? ??????
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "????????? ?????? ?????? ??????", error)
            } else if (user != null) {
                Log.e(TAG, "????????? ?????? ?????? ??????")

                //????????? ?????? ??????
                myKakaoId = user.id
                myEmail = user.kakaoAccount?.email
                myPicture = user.kakaoAccount?.profile?.profileImageUrl
                myNickname = user.kakaoAccount?.profile?.nickname

                invite_pref = getSharedPreferences("invite_pref", Context.MODE_PRIVATE)

                if (invite_pref.getLong("invite_id", 0)?.toInt() != 0){
                    var id = invite_pref.getLong("invite_id", 0)
                    var roomIdx = invite_pref.getLong("invite_roomIdx", 0)
                    var picture = invite_pref.getString("invite_picture", "")
                    var nickname = invite_pref.getString("invite_nickname", "")

                    inviteDialogOn(id, roomIdx, picture, nickname)
                }
                joinFriendList.add(FriendData(myKakaoId,ImageData(joinFriendList.size, myPicture!!), myNickname!!))
                setProfileRecyclerview()

                var requestData = JSONObject()
                requestData.put("id", myKakaoId)


                // ???????????? ?????? ?????? ???????????? (??????)
                TalkApiClient.instance.friends { friends, error ->
                    if (error != null) {
                        Log.e(TAG, "???????????? ?????? ?????? ???????????? ??????", error)
                    }
                    else if (friends != null) {

                        val friendList = JSONArray()

                        if (friends.totalCount > 0){
                            for (friend in friends.elements!!) {
                                val friendObj = JSONObject()
                                friendObj.put("id", friend.id)
                                friendObj.put("uuid", friend.uuid)
                                friendObj.put("profile_nickname", friend.profileNickname)
                                friendObj.put("profile_thumbnail_image", friend.profileThumbnailImage)
                                friendObj.put("favorite", friend.favorite)
                                friendObj.put("allowedMsg", friend.allowedMsg)
                                friendList.put(friendObj)
                            }
                        }
                        requestData.put("elements", friendList)
                        mSocket?.emit("join", requestData)
                        
                        val jsonObject = JSONObject()
                        jsonObject.put("id", myKakaoId)
                        jsonObject.put("nickname", myNickname)
                        jsonObject.put("picture", myPicture)
                        mSocket?.emit("participate", jsonObject)
                        
                    }
                }



                // ??????????????? ?????? ?????? ??? ?????? ????????? ?????? picat?????? ???????????? ???
                if(intent.type == "image/*") {
                    if (intent.action == Intent.ACTION_SEND){
                        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        var file = File(getAbsolutePath(imageUri, this))
                        var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                        var emitBody: MutableList<MultipartBody.Part>? = mutableListOf()
                        var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                        emitBody?.add(body)
                        apiRequest(emitBody, 1)
                    }
                    else if(intent.action == Intent.ACTION_SEND_MULTIPLE){
                        val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)

                        val count: Int? = imageUriList?.toArray()?.size
                        var emitBody: MutableList<MultipartBody.Part>? = mutableListOf()
                        for (index in 0 until count!!) {
                            var file = File(getAbsolutePath(imageUriList[index], this))
                            var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                            var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                            emitBody?.add(body)
                        }
                        apiRequest(emitBody, count)
                    }
                }


            }
        }

        // ????????? ?????? ??????
        val actionbar = binding.toolbar
        setSupportActionBar(actionbar) //???????????? toolbar??? ???????????? ??????
        supportActionBar?.setDisplayShowTitleEnabled(false) //???????????? ???????????? ????????? ??????????????? ???????????????. false??? ?????? custom??? ????????? ????????? ????????? ????????? ?????????.

        // ????????? ?????? ??? ????????? ??????: ????????? ?????? ?????? ??????
        binding.profileItemPlusButton.setOnClickListener {
            val openPickerFriendRequestParams = OpenPickerFriendRequestParams(
                title = "?????? ????????????", //default "?????? ??????"
                viewAppearance = ViewAppearance.AUTO, //default ViewAppearance.AUTO
                orientation = PickerOrientation.AUTO, //default PickerOrientation.AUTO
                enableSearch = true, //default true
                enableIndex = true, //default true
                showMyProfile = false, //default true
                showFavorite = true, //default true
                showPickedFriend = null, // default true
                maxPickableCount = null, // default 30
                minPickableCount = null // default 1
            )

            PickerClient.instance.selectFriends(
                context = this!!,
                params = openPickerFriendRequestParams
            ) { selectedUsers, error ->
                if (error != null) {
                    Log.e(TAG, "?????? ?????? ??????", error)
                } else {
                    Log.d(TAG, "?????? ?????? ?????? $selectedUsers")
                    val friend_count = selectedUsers?.totalCount
                    var inviteFriendsId = mutableSetOf<Long>()
                    for (i in 0 until friend_count!!) {
                        inviteFriendsId.add(selectedUsers?.users?.get(i)?.id!!)
                    }
                    inviteRequest(inviteFriendsId)
                }
            }
        }

        //Adapter ?????????
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        profilePictureAdapter = ProfilePictureAdapter(joinFriendList, this)
        profilePictureAdapter.setClickListener(object : ProfilePictureAdapter.ItemClickListener{
            override fun onItemClick(view: View?, position: Int) {
                gotoFaceFilter(position)
            }
        })


        //recyclerview ???????????? ??????
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)
        pictureAdapter = PictureAdapter(imageDataList, this, selectionIdList)
        binding.pictureRecyclerview.adapter = pictureAdapter
        binding.pictureRecyclerview.addItemDecoration(SpaceItemDecorator(left = 10, top = 10, right = 10, bottom = 10))



        //???????????? ?????????
        bottomSheetDialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialog)
        val bottomSheetView = LayoutInflater.from(applicationContext)
            .inflate(R.layout.bottomsheet_content, findViewById<ConstraintLayout>(R.id.bottomsheet_layout))

        bottomSheetDialog.setContentView(bottomSheetView)

        // fab?????? ?????? ??? ???????????? ?????????
        binding.openBottomsheetFab.setOnClickListener {


            bottomSheetDialog.show()
        }



//        binding.linkButton.setOnClickListener {
//            val url = "http://13.124.233.208:5000/"
//            val defaultText = TextTemplate(
//                text = """
//                    Picat??? ?????? ???????????? ????????? ???????????? ??????????????????!
//                    """.trimIndent(),
//                link = Link(
//                    mobileWebUrl = "https://developers.kakao.com"
//                )
//            )
//
//            // ???????????? ???????????? ??????
//            if (ShareClient.instance.isKakaoTalkSharingAvailable(this)) {
//                // ?????????????????? ???????????? ?????? ??????
//                ShareClient.instance.shareScrap(this, url) { sharingResult, error ->
//                    if (error != null) {
//                        Log.e(TAG, "???????????? ?????? ??????", error)
//                    }
//                    else if (sharingResult != null) {
//                        Log.d(TAG, "???????????? ?????? ?????? ${sharingResult.intent}")
//                        startActivity(sharingResult.intent)
//
//                        // ???????????? ????????? ??????????????? ?????? ?????? ???????????? ????????? ?????? ?????? ???????????? ?????? ???????????? ?????? ??? ????????????.
//                        Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
//                        Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
//                    }
//                }
//            } else {
//                // ???????????? ?????????: ??? ?????? ?????? ??????
//                // ??? ?????? ?????? ??????
//                val sharerUrl = WebSharerClient.instance.makeScrapUrl(url)
//
//                // CustomTabs?????? ??? ???????????? ??????
//
//                // 1. CustomTabsServiceConnection ?????? ???????????? ??????
//                // ex) Chrome, ?????? ?????????, FireFox, ?????? ???
//                try {
//                    KakaoCustomTabsClient.openWithDefault(this, sharerUrl)
//                } catch(e: UnsupportedOperationException) {
//                    // CustomTabsServiceConnection ?????? ??????????????? ?????? ??? ????????????
//                }
//
//                // 2. CustomTabsServiceConnection ????????? ???????????? ??????
//                // ex) ??????, ????????? ???
//                try {
//                    KakaoCustomTabsClient.open(this, sharerUrl)
//                } catch (e: ActivityNotFoundException) {
//                    // ??????????????? ????????? ????????? ??????????????? ?????? ??? ????????????
//                }
//            }
//        }

        // ?????? ????????? ????????? ?????? ????????? ????????????
        if(switch_pref.getBoolean("store_check", false)) {
            binding.autoUploadSwitch.isChecked = true
        }

        // ?????? ????????? ????????? ??????/??????
        binding.autoUploadSwitch.setOnCheckedChangeListener { p0, isChecked ->
            if (isChecked) {
                val status = ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
                if (status == PackageManager.PERMISSION_GRANTED) {
                    // ?????????????????? ??????
                    val serviceIntent = Intent(this, ForegroundService::class.java)
                    serviceIntent.putExtra("myKakaoId", myKakaoId)
                    switch_pref.edit().putBoolean("store_check", true).apply()
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
                else {
                    // permission ?????? ?????? ??????
                    requestPermissionLauncher.launch("android.permission.CAMERA")
                }
            }
            else {
                // ?????????????????? ??????
                val serviceIntent = Intent(this, ForegroundService::class.java)
                stopService(serviceIntent)
                switch_pref.edit().putBoolean("store_check", false).apply()
            }
        }

        // ?????? ?????? ??????
        var entireButton = binding.allFilterButton
        entireButton.setOnClickListener {
            setRecyclerView()
        }

        // ?????? ?????? ?????? ??????
        var exceptBlurButton = binding.exceptBlurFilterButton
        exceptBlurButton.setOnClickListener {
            clearImageDataList.clear()

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.picat_server))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // APIInterface ?????? ??????
            var server: RequestInterface = retrofit.create(RequestInterface::class.java)
            server.getExceptBlur(myKakaoId).enqueue(object : Callback<ImageResponseData> {

                override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                    if (response.isSuccessful) {
                        for (imgData in imageDataList) {
                            for (url in response.body()?.url!!) {
                                if (imgData.uri.equals(url)) {
                                    clearImageDataList.add(imgData)
                                }
                            }
                        }
                        setClearRecyclerView()
                    }
                }
                override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                    println("????????? ????????? ??????")
                }
            })
        }

        // ?????? ????????? ?????? ??????
        var onlyBlurButton = binding.onlyBlurFilterButton
        onlyBlurButton.setOnClickListener {
            blurImageDataList.clear()

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.picat_server))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // APIInterface ?????? ??????
            var server: RequestInterface = retrofit.create(RequestInterface::class.java)
            server.getBlur(myKakaoId).enqueue(object : Callback<ImageResponseData> {

                override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                    if (response.isSuccessful) {
                        for (imgData in imageDataList) {
                            for (url in response.body()?.url!!) {
                                if (imgData.uri.equals(url)) {
                                    blurImageDataList.add(imgData)
                                }
                            }
                        }
                        setBlurRecyclerView()
                    }
                }

                override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                    println("????????? ????????? ??????")
                }
            })
        }


        //???????????? ??? ????????? ??????
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_upload_button).setOnClickListener {
            // permission ?????? ??????
            val status = ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
            if (status == PackageManager.PERMISSION_GRANTED) {
                // ????????? ??????
                val intent = Intent(Intent.ACTION_PICK)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                activityResult.launch(intent)
                bottomSheetDialog.hide()
            }
            else {
                // permission ?????? ?????? ??????
                requestPermissionLauncher.launch("android.permission.READ_EXTERNAL_STORAGE")
            }
        }

        // ???????????? ?????? ????????? ??? ?????? alert ??????
        val builder = AlertDialog.Builder(this, R.style.BasicDialogTheme)
        val createAlbumAlertView = LayoutInflater.from(this)
            .inflate(R.layout.basic_alert_content, findViewById<ConstraintLayout>(R.id.basic_alert_layout))
        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = builder.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val roomNameEditText = binding.roomNameEditText


        //???????????? ??? ???????????? ??????
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_download_button).setOnClickListener {
            bottomSheetDialog.dismiss()

            requestPermissionLauncher.launch("android.permission.WRITE_EXTERNAL_STORAGE")

            var roomName = roomNameEditText.text.toString()
            if(roomName.isEmpty()){
                Toast.makeText(this, "?????? ????????? ??????????????????", Toast.LENGTH_SHORT).show()
                roomNameEditText.requestFocus()
            }else{
                roomNameEditText.clearFocus()
                createAlbumAlertView.findViewById<TextView>(R.id.alert_title).text = "\"${roomName}\" ${getString(R.string.download_album_alert_title)}"
                createAlbumAlertView.findViewById<TextView>(R.id.alert_subtitle).text = getString(R.string.download_album_alert_subtitle)

                alertDialog?.show()
            }
        }

        //???????????? ??? ???????????? ??????
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_select_button).setOnClickListener {
            bottomSheetDialog.hide()
            val intent = Intent(this, SelectPictureActivity::class.java)
            intent.putExtra("selectonIdList", selectionIdList)
            intent.putExtra("myKakaoId", myKakaoId)

            if (binding.allFilterButton.isChecked == true) {
                intent.putExtra("imageDataList", imageDataList)
            }
            else if (binding.exceptBlurFilterButton.isChecked == true) {
                intent.putExtra("imageDataList", clearImageDataList)
            }
            else {
                intent.putExtra("imageDataList", blurImageDataList)
            }

            activityResult.launch(intent)

        }

        // ???????????? ??????
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_alert).setOnClickListener {
            alertDialog?.dismiss()
        }

        // ???????????? ??????
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_alert).setOnClickListener {
            alertDialog?.dismiss()
            imageDownload(binding.roomNameEditText.text.toString())

            val bundle = Bundle()
            bundle.putInt("pictureCount", pictureAdapter.mSelected.size)
            bundle.putString("albumName", binding.roomNameEditText.text.toString())
            bundle.putString("albumCover", imageDataList[pictureAdapter.mSelected.first()].uri)
            val downloadAlbumFragment = DownloadCompleteFragment()
            downloadAlbumFragment.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction().add(R.id.activity_share_picture_layout, downloadAlbumFragment)
            transaction.commit()
        }
    }

    override fun onResume() {
        super.onResume()

        if (mSocket != null && !mSocket?.isActive!!) {
            mSocket?.connect()
//            mSocket?.emit("participate", myKakaoId)
//            mSocket?.on("participate", onParticipate)
        }
    }

    // ?????? ?????? parsing
    fun getFileNameInUrl(imgUrl: String): String{
        val ary = imgUrl.split("/")
        return ary[3]
    }

    // ???????????? ????????? ????????????
    private fun imageDownload(albumName: String){
        val savePath: String = Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).toString() + "/" + albumName
        val dir = File(savePath)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        for (pos in selectionIdList){
            val imgUrl = imageDataList[pos].uri.toString()
            val fileName = getFileNameInUrl(imgUrl)
            val localPath = "$savePath/$fileName"

            val imgDownloadCoroutine = CoroutineScope(Dispatchers.IO)
            imgDownloadCoroutine.launch {
                //url??? ????????? ???????????? ???????????? ????????????
                val conn: HttpURLConnection = URL(imgUrl).openConnection() as HttpURLConnection
                val len: Int = conn.contentLength
                val tmpByte = ByteArray(len)
                val inputStream : InputStream = conn.inputStream

                val file = File(localPath)
                val outputStream = FileOutputStream(file)
                var readData: Int
                while (true) {
                    readData = inputStream.read(tmpByte)
                    if (readData <= 0) {
                        break
                    }
                    outputStream.write(tmpByte, 0, readData)
                }
                MediaScannerConnection.scanFile(applicationContext, arrayOf(dir.toString()), null, null)
                inputStream.close()
                outputStream.close()
                conn.disconnect()
            }
        }
    }

    // ?????? ????????????
    private val activityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            // ??????????????? ???????????? ?????? ??? ????????? ??????
            if (it.data!!.clipData != null) {
                val count = it.data!!.clipData!!.itemCount

                var emitBody: MutableList<MultipartBody.Part>? = mutableListOf()
                for (index in 0 until count) {
                    val imageUri = it.data!!.clipData!!.getItemAt(index).uri

                    var file = File(getAbsolutePath(imageUri, this))
                    var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                    var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    emitBody?.add(body)
                }
                apiRequest(emitBody, count)
            }
        }
        else if (it.resultCode == 1){
            //??????????????? ?????? ????????? ???????????? ????????? ??????
            if (it.data!!.hasExtra("selectonIdList")){
                selectionIdList = it.data!!.getSerializableExtra("selectonIdList")!! as HashSet<Int>
                if (binding.allFilterButton.isChecked == true) {
                    setRecyclerView()
                }
                else if (binding.exceptBlurFilterButton.isChecked == true) {
                    setClearRecyclerView()
                }
                else {
                    setBlurRecyclerView()
                }
            }
        }
    }

    // ????????? ?????? ?????? ????????????
    private fun getAbsolutePath(path: Uri?, context : Context): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        var index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        var result = c?.getString(index!!)

        return result!!
    }

    // ????????? ????????? post ??????
    private fun apiRequest(image_multipart: MutableList<MultipartBody.Part>?, img_cnt: Int) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        // retrofit ?????? ??????
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface ?????? ??????
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)

        progressOn()

        server.postImg(myKakaoId!!, image_multipart!!, img_cnt, myKakaoId!!).enqueue(object : Callback<ImageResponseData> {
            override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                progressOff()
                if (response.isSuccessful){
                    val jsonArray = JSONArray()

                    for (i in 0 until response.body()?.img_cnt!!) {
                        val jsonObject = JSONObject()
                        jsonObject.put("url", response.body()?.url?.toList()?.get(i))
                        jsonArray.put(jsonObject)
                    }

                    var friendJSON = JSONArray(response.body()?.friends.toString())

                    var friendDataList : ArrayList<FriendData> = ArrayList()

                    for (i in 0 until friendJSON.length()) {
                        var nickName = (friendJSON.getJSONObject(i).getString("nickname"))
                        var id = (friendJSON.getJSONObject(i).getLong("id"))
                        var imgObj = (friendJSON.getJSONObject(i).getString("picture").toUri())
                        var imgData = ImageData(i, imgObj.toString())
                        var friendData = FriendData(id, imgData, nickName)
                        friendDataList.add(friendData)
                        friendDataList.distinct()
                    }

                    if (friendJSON.length() > 0) {
                        openInviteDialog(friendDataList)
                    }

                    val jsonObject = JSONObject()
                    jsonObject.put("img_list", jsonArray)
                    jsonObject.put("img_cnt", response.body()?.img_cnt)
                    jsonObject.put("id", myKakaoId)

                    mSocket?.emit("image", jsonObject)
                }
            }

            override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                progressOff()
                println("????????? ????????? ??????")
            }
        })
    }

    // profile recyclerview ?????????
    private fun setProfileRecyclerview(){
        // profile picture recyclerview ??????
        profilePictureAdapter = ProfilePictureAdapter(joinFriendList, this)
        profilePictureAdapter.setClickListener(object : ProfilePictureAdapter.ItemClickListener{
            override fun onItemClick(view: View?, position: Int) {
                // ?????? ??? selectPictureActivity??? ???????????? -> ????????? ?????? api ??????
                gotoFaceFilter(position)
            }

        })
        binding.profileRecyclerview.adapter = profilePictureAdapter
    }

    // recyclerview ?????????
    private fun setRecyclerView(){
        // picture recyclerview ??????
        pictureAdapter = PictureAdapter(imageDataList, this, selectionIdList)
        pictureAdapter.setClickListener(object : PictureAdapter.ItemClickListener{
            override fun onItemLongClick(view: View?, position: Int): Boolean {
                val intent = Intent(applicationContext, SelectPictureActivity::class.java)
                intent.putExtra("selectonIdList", selectionIdList)
                intent.putExtra("myKakaoId", myKakaoId)

                if (binding.allFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", imageDataList)
                }
                else if (binding.exceptBlurFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", clearImageDataList)
                }
                else {
                    intent.putExtra("imageDataList", blurImageDataList)
                }

                activityResult.launch(intent)
                return true
            }

        })
        binding.pictureRecyclerview.adapter = pictureAdapter
    }

    // ?????? ?????? recyclerview ?????????
    private fun setBlurRecyclerView() {
        pictureAdapter = PictureAdapter(blurImageDataList, this, selectionIdList)
        pictureAdapter.setClickListener(object : PictureAdapter.ItemClickListener{
            override fun onItemLongClick(view: View?, position: Int): Boolean {
                val intent = Intent(applicationContext, SelectPictureActivity::class.java)
                intent.putExtra("selectonIdList", selectionIdList)
                intent.putExtra("myKakaoId", myKakaoId)

                if (binding.allFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", imageDataList)
                }
                else if (binding.exceptBlurFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", clearImageDataList)
                }
                else {
                    intent.putExtra("imageDataList", blurImageDataList)
                }

                activityResult.launch(intent)
                return true
            }

        })
        binding.pictureRecyclerview.adapter = pictureAdapter
    }

    // ?????? ?????? ?????? recyclerview ?????????
    private fun setClearRecyclerView() {
        pictureAdapter = PictureAdapter(clearImageDataList, this, selectionIdList)
        pictureAdapter.setClickListener(object : PictureAdapter.ItemClickListener{
            override fun onItemLongClick(view: View?, position: Int): Boolean {
                val intent = Intent(applicationContext, SelectPictureActivity::class.java)
                intent.putExtra("selectonIdList", selectionIdList)
                intent.putExtra("myKakaoId", myKakaoId)

                if (binding.allFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", imageDataList)
                }
                else if (binding.exceptBlurFilterButton.isChecked == true) {
                    intent.putExtra("imageDataList", clearImageDataList)
                }
                else {
                    intent.putExtra("imageDataList", blurImageDataList)
                }

                activityResult.launch(intent)
                return true
            }

        })
        binding.pictureRecyclerview.adapter = pictureAdapter
    }

    // ?????? ?????? ?????? ??????
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    // ?????? ?????? ??????
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            // ????????? ??????
            R.id.out_button -> {
                exitDialogOn()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ???????????? 2???
    override fun onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "?????? ????????? ?????? ??? ????????? ???????????????.", Toast.LENGTH_SHORT).show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            bottomSheetDialog.dismiss()
            mSocket?.disconnect()
            mSocket?.close()
            finish()
        }
    }


    // ????????? url??? ????????? ???
    var onImage = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
                val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
                for (i in 0..img_count - 1) {
                    val imgObj = JSONObject(img_list[i].toString()).getString("url")
                    val imgData = ImageData(imageDataList.size, imgObj)
                    imageDataList.add(imgData)
                }
                if (binding.allFilterButton.isChecked == true) {
                    setRecyclerView()
                }
            }
        }
    }

    // ?????? ???????????? ???
    var onJoin = Emitter.Listener { args->
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
                val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
                for (i in 0 until img_count) {
                    val imgObj = JSONObject(img_list[i].toString()).getString("url")
                    val imgData = ImageData(imageDataList.size, imgObj)
                    imageDataList.add(imgData)
                }
                setRecyclerView()
            }
        }
    }

    // ?????? ????????? ???????????? ???
    var onParticipate = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                joinFriendList?.clear()
                var friendList = JSONObject(args[0].toString()).getJSONArray("friends_list")
                for (i in 0..friendList.length() - 1) {
                    val jsonObject = JSONObject(friendList[i].toString())

                    val profile = jsonObject.getString("picture")
                    val id = jsonObject.getLong("id")
                    val nickName = jsonObject.getString("nickname")

                    joinFriendList.add(FriendData(id, ImageData(joinFriendList.size, profile), nickName))
                }
                setProfileRecyclerview()
            }
        }
    }

    var onExit = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock {
                val id = args[0].toString().toLong()

                for ( friend in joinFriendList){
                    if (friend.id == id){
                        joinFriendList.remove(friend)
                        break
                    }
                }
                setProfileRecyclerview()
            }
        }
    }

    // ?????? ?????? ??????????????? ??????
    fun openInviteDialog(friendDataList : ArrayList<FriendData>) {
        val dialog = InviteDialog(this)
        dialog.setOnOKClickedListener { content ->

            //????????? ????????? ?????? ???
            if(content != null) {
                var sendFriendId = mutableSetOf<Long>()
                for (i in content) {
                    sendFriendId.add(friendDataList[i].id!!)
                }
                if (sendFriendId.isNotEmpty()){
                    inviteRequest(sendFriendId)
                }
            }
        }
        dialog.show(friendDataList)
    }

    // ?????? ?????? post ??????
    private fun inviteRequest(friendsId: MutableSet<Long>) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface ?????? ??????
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postFriends(friendsId!!, myKakaoId!!).enqueue(object : Callback<SimpleResponseData> {

            override fun onResponse(call: Call<SimpleResponseData>, response: Response<SimpleResponseData>) {
                if (response.body()?.isSuccess!!) {
                    println("?????? ?????? ??????")
                }
            }

            override fun onFailure(call: Call<SimpleResponseData>, t: Throwable) {
                println("?????? ?????? ??????")
            }
        })
    }

    //???????????? ???????????? ????????????
    fun gotoFaceFilter(position: Int) {
        var intent = Intent(applicationContext, SelectByPeopleActivity::class.java)
        intent.putExtra("friendId", joinFriendList[position].id)
        intent.putExtra("selectionIdList", selectionIdList)
        intent.putExtra("imageDataList", imageDataList)
        intent.putExtra("selected_profile_image", joinFriendList[position].picture.uri)
        setResult(1)
        activityResult.launch(intent)
    }

    // ?????? ??????????????? ??????
    fun progressOn() {
        val binding = LoadingDialogBinding.inflate(layoutInflater)
        progressDialog = AppCompatDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.setContentView(binding.root)
        Glide.with(this).load(R.raw.loading_cat).into(binding.loadingImageview)
        progressDialog.show()
    }

    // ?????? ??????????????? ??????
    fun progressOff() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
    }

    // ????????? ??????????????? ??????
    fun exitDialogOn() {
        val binding = ExitDialogBinding.inflate(layoutInflater)
        exitDialog = AppCompatDialog(this)
        exitDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        exitDialog.setContentView(binding.root)
        binding.exitCancelButton.setOnClickListener {
            exitDialog.dismiss()
        }
        binding.exitCheckButton.setOnClickListener {
            mSocket?.emit("exit", myKakaoId)
            bottomSheetDialog.dismiss()
            exitDialog.dismiss()

//            UserApiClient.instance.unlink { error ->
//                if (error != null) {
//                    println("?????? ?????? ??????.")
//                }
//                else {
//                    println("?????? ?????? ??????. SDK?????? ?????? ?????????")
//                }
//            }
            finish()
        }
        exitDialog.show()
    }

    // ?????? ?????? ??????????????? ??????
    fun inviteDialogOn(id: Long?, roomIdx: Long?, picture: String?, nickname: String?) {
        val binding = InviteCheckDialogBinding.inflate(layoutInflater)
        inviteDialog = AppCompatDialog(this)
        inviteDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        Glide.with(this).load(picture).circleCrop().into(binding.friendPicture)
        binding.kakaoNickName.text = nickname
        binding.inviteRoomTitle.text = "${nickname}?????? ????????? ????????????"
        binding.inviteRoomSubtitle.text = "${nickname}?????? ????????? ????????????????"
        inviteDialog.setContentView(binding.root)
        inviteDialog.show()
        binding.inviteAcceptButton.setOnClickListener {
            // ??? id???, ????????? ??? ?????? post ?????????
            postInviteResponse(id, roomIdx, picture, nickname)
            invite_pref.edit().clear().commit()
            inviteDialog.dismiss()
        }
        binding.inviteCancelButton.setOnClickListener {
            inviteDialog.dismiss()
            invite_pref.edit().clear().commit()
        }
    }

    // ?????? ?????? ?????? post ??????
    private fun postInviteResponse(id: Long?, roomIdx: Long?, picture: String?, nickname: String?) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface ?????? ??????
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postInviteResponse(myKakaoId, roomIdx).enqueue(object : Callback<SimpleResponseData> {

            override fun onResponse(call: Call<SimpleResponseData>, response: Response<SimpleResponseData>) {
                if (response.body()?.isSuccess!!) {
                    finishAffinity()
                    val intent = Intent(applicationContext, SharePictureActivity::class.java)
                    startActivity(intent)
                    System.exit(0)
                }
            }

            override fun onFailure(call: Call<SimpleResponseData>, t: Throwable) {
                println("?????? ?????? ??????")
            }
        })
    }

}



