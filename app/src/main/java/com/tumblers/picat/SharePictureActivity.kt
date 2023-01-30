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

    // 유저 데이터
    var myKakaoId : Long? = null
    var myNickname: String? = ""
    var myPicture: String? = ""
    var myEmail: String? = ""

    //뒤로가기 타이머
    var backKeyPressedTime: Long = 0

    // switch 상태 저장
    lateinit var switch_pref : SharedPreferences
    lateinit var invite_pref : SharedPreferences

    private lateinit var progressDialog: AppCompatDialog
    private lateinit var exitDialog: AppCompatDialog
    private lateinit var inviteDialog: AppCompatDialog

    // 임계구역에 있는 경우 동시성 발생 X
    val mutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // socket 통신 연결
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


        // 토큰 정보 확인 후
        // 실패 시 로그인 화면으로 이동
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                val intent = Intent(this, LoginActivity::class.java)
                finish()
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            else if (tokenInfo != null) {
                //앱 진입 시 퍼미션 요청
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

        // switch 버튼 체크 유무 저장
        switch_pref = getSharedPreferences("switch_pref", Context.MODE_PRIVATE)

        // 사용자 정보 요청 및 소켓 연결
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.e(TAG, "사용자 정보 요청 성공")

                //사용자 정보 요청
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


                // 카카오톡 친구 목록 가져오기 (기본)
                TalkApiClient.instance.friends { friends, error ->
                    if (error != null) {
                        Log.e(TAG, "카카오톡 친구 목록 가져오기 실패", error)
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



                // 갤러리에서 사진 선택 후 공유 버튼을 눌러 picat앱에 들어왔을 때
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

        // 액션바 제목 설정
        val actionbar = binding.toolbar
        setSupportActionBar(actionbar) //커스텀한 toolbar를 액션바로 사용
        supportActionBar?.setDisplayShowTitleEnabled(false) //액션바에 표시되는 제목의 표시유무를 설정합니다. false로 해야 custom한 툴바의 이름이 화면에 보이게 됩니다.

        // 프로필 사진 옆 플러스 버튼: 카카오 친구 피커 실행
        binding.profileItemPlusButton.setOnClickListener {
            val openPickerFriendRequestParams = OpenPickerFriendRequestParams(
                title = "친구 초대하기", //default "친구 선택"
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
                    Log.e(TAG, "친구 선택 실패", error)
                } else {
                    Log.d(TAG, "친구 선택 성공 $selectedUsers")
                    val friend_count = selectedUsers?.totalCount
                    var inviteFriendsId = mutableSetOf<Long>()
                    for (i in 0 until friend_count!!) {
                        inviteFriendsId.add(selectedUsers?.users?.get(i)?.id!!)
                    }
                    inviteRequest(inviteFriendsId)
                }
            }
        }

        //Adapter 초기화
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        profilePictureAdapter = ProfilePictureAdapter(joinFriendList, this)
        profilePictureAdapter.setClickListener(object : ProfilePictureAdapter.ItemClickListener{
            override fun onItemClick(view: View?, position: Int) {
                gotoFaceFilter(position)
            }
        })


        //recyclerview 레이아웃 설정
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)
        pictureAdapter = PictureAdapter(imageDataList, this, selectionIdList)
        binding.pictureRecyclerview.adapter = pictureAdapter
        binding.pictureRecyclerview.addItemDecoration(SpaceItemDecorator(left = 10, top = 10, right = 10, bottom = 10))



        //바텀시트 초기화
        bottomSheetDialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialog)
        val bottomSheetView = LayoutInflater.from(applicationContext)
            .inflate(R.layout.bottomsheet_content, findViewById<ConstraintLayout>(R.id.bottomsheet_layout))

        bottomSheetDialog.setContentView(bottomSheetView)

        // fab버튼 클릭 시 바텀시트 활성화
        binding.openBottomsheetFab.setOnClickListener {


            bottomSheetDialog.show()
        }



//        binding.linkButton.setOnClickListener {
//            val url = "http://13.124.233.208:5000/"
//            val defaultText = TextTemplate(
//                text = """
//                    Picat을 통해 친구들과 사진을 편리하게 공유해보세요!
//                    """.trimIndent(),
//                link = Link(
//                    mobileWebUrl = "https://developers.kakao.com"
//                )
//            )
//
//            // 카카오톡 설치여부 확인
//            if (ShareClient.instance.isKakaoTalkSharingAvailable(this)) {
//                // 카카오톡으로 카카오톡 공유 가능
//                ShareClient.instance.shareScrap(this, url) { sharingResult, error ->
//                    if (error != null) {
//                        Log.e(TAG, "카카오톡 공유 실패", error)
//                    }
//                    else if (sharingResult != null) {
//                        Log.d(TAG, "카카오톡 공유 성공 ${sharingResult.intent}")
//                        startActivity(sharingResult.intent)
//
//                        // 카카오톡 공유에 성공했지만 아래 경고 메시지가 존재할 경우 일부 컨텐츠가 정상 동작하지 않을 수 있습니다.
//                        Log.w(TAG, "Warning Msg: ${sharingResult.warningMsg}")
//                        Log.w(TAG, "Argument Msg: ${sharingResult.argumentMsg}")
//                    }
//                }
//            } else {
//                // 카카오톡 미설치: 웹 공유 사용 권장
//                // 웹 공유 예시 코드
//                val sharerUrl = WebSharerClient.instance.makeScrapUrl(url)
//
//                // CustomTabs으로 웹 브라우저 열기
//
//                // 1. CustomTabsServiceConnection 지원 브라우저 열기
//                // ex) Chrome, 삼성 인터넷, FireFox, 웨일 등
//                try {
//                    KakaoCustomTabsClient.openWithDefault(this, sharerUrl)
//                } catch(e: UnsupportedOperationException) {
//                    // CustomTabsServiceConnection 지원 브라우저가 없을 때 예외처리
//                }
//
//                // 2. CustomTabsServiceConnection 미지원 브라우저 열기
//                // ex) 다음, 네이버 등
//                try {
//                    KakaoCustomTabsClient.open(this, sharerUrl)
//                } catch (e: ActivityNotFoundException) {
//                    // 디바이스에 설치된 인터넷 브라우저가 없을 때 예외처리
//                }
//            }
//        }

        // 자동 업로드 스위치 버튼 저장값 불러오기
        if(switch_pref.getBoolean("store_check", false)) {
            binding.autoUploadSwitch.isChecked = true
        }

        // 자동 업로드 스위치 켜기/끄기
        binding.autoUploadSwitch.setOnCheckedChangeListener { p0, isChecked ->
            if (isChecked) {
                val status = ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
                if (status == PackageManager.PERMISSION_GRANTED) {
                    // 포어그라운드 실행
                    val serviceIntent = Intent(this, ForegroundService::class.java)
                    serviceIntent.putExtra("myKakaoId", myKakaoId)
                    switch_pref.edit().putBoolean("store_check", true).apply()
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
                else {
                    // permission 허용 요청 실행
                    requestPermissionLauncher.launch("android.permission.CAMERA")
                }
            }
            else {
                // 포어그라운드 종료
                val serviceIntent = Intent(this, ForegroundService::class.java)
                stopService(serviceIntent)
                switch_pref.edit().putBoolean("store_check", false).apply()
            }
        }

        // 전체 사진 버튼
        var entireButton = binding.allFilterButton
        entireButton.setOnClickListener {
            setRecyclerView()
        }

        // 흐린 사진 제외 버튼
        var exceptBlurButton = binding.exceptBlurFilterButton
        exceptBlurButton.setOnClickListener {
            clearImageDataList.clear()

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.picat_server))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // APIInterface 객체 생성
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
                    println("이미지 업로드 실패")
                }
            })
        }

        // 흐린 사진만 보기 버튼
        var onlyBlurButton = binding.onlyBlurFilterButton
        onlyBlurButton.setOnClickListener {
            blurImageDataList.clear()

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.picat_server))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // APIInterface 객체 생성
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
                    println("이미지 업로드 실패")
                }
            })
        }


        //바텀시트 내 업로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_upload_button).setOnClickListener {
            // permission 허용 확인
            val status = ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
            if (status == PackageManager.PERMISSION_GRANTED) {
                // 갤러리 호출
                val intent = Intent(Intent.ACTION_PICK)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                activityResult.launch(intent)
                bottomSheetDialog.hide()
            }
            else {
                // permission 허용 요청 실행
                requestPermissionLauncher.launch("android.permission.READ_EXTERNAL_STORAGE")
            }
        }

        // 다운로드 버튼 눌렀을 때 띄울 alert 생성
        val builder = AlertDialog.Builder(this, R.style.BasicDialogTheme)
        val createAlbumAlertView = LayoutInflater.from(this)
            .inflate(R.layout.basic_alert_content, findViewById<ConstraintLayout>(R.id.basic_alert_layout))
        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = builder.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val roomNameEditText = binding.roomNameEditText


        //바텀시트 내 다운로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_download_button).setOnClickListener {
            bottomSheetDialog.dismiss()

            requestPermissionLauncher.launch("android.permission.WRITE_EXTERNAL_STORAGE")

            var roomName = roomNameEditText.text.toString()
            if(roomName.isEmpty()){
                Toast.makeText(this, "앨범 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                roomNameEditText.requestFocus()
            }else{
                roomNameEditText.clearFocus()
                createAlbumAlertView.findViewById<TextView>(R.id.alert_title).text = "\"${roomName}\" ${getString(R.string.download_album_alert_title)}"
                createAlbumAlertView.findViewById<TextView>(R.id.alert_subtitle).text = getString(R.string.download_album_alert_subtitle)

                alertDialog?.show()
            }
        }

        //바텀시트 내 사진선택 버튼
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

        // 다운로드 취소
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_alert).setOnClickListener {
            alertDialog?.dismiss()
        }

        // 다운로드 확인
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

    // 파일 이름 parsing
    fun getFileNameInUrl(imgUrl: String): String{
        val ary = imgUrl.split("/")
        return ary[3]
    }

    // 이미지를 로컬로 다운로드
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
                //url로 서버와 접속해서 이미지를 다운받기
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

    // 결과 가져오기
    private val activityResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            // 갤러리에서 이미지가 선택 후 돌아온 경우
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
            //사진선택을 통해 이미지 선택해서 돌아온 경우
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

    // 사진의 절대 경로 가져오기
    private fun getAbsolutePath(path: Uri?, context : Context): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        var index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        var result = c?.getString(index!!)

        return result!!
    }

    // 이미지 업로드 post 요청
    private fun apiRequest(image_multipart: MutableList<MultipartBody.Part>?, img_cnt: Int) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface 객체 생성
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
                println("이미지 업로드 실패")
            }
        })
    }

    // profile recyclerview 초기화
    private fun setProfileRecyclerview(){
        // profile picture recyclerview 설정
        profilePictureAdapter = ProfilePictureAdapter(joinFriendList, this)
        profilePictureAdapter.setClickListener(object : ProfilePictureAdapter.ItemClickListener{
            override fun onItemClick(view: View?, position: Int) {
                // 클릭 시 selectPictureActivity로 넘어가며 -> 넘어간 이후 api 호출
                gotoFaceFilter(position)
            }

        })
        binding.profileRecyclerview.adapter = profilePictureAdapter
    }

    // recyclerview 초기화
    private fun setRecyclerView(){
        // picture recyclerview 설정
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

    // 흐린 사진 recyclerview 초기화
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

    // 흐린 사진 제외 recyclerview 초기화
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

    // 툴바 메뉴 버튼 설정
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    // 툴바 메뉴 버튼
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            // 나가기 버튼
            R.id.out_button -> {
                exitDialogOn()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 뒤로가기 2번
    override fun onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "뒤로 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            bottomSheetDialog.dismiss()
            mSocket?.disconnect()
            mSocket?.close()
            finish()
        }
    }


    // 이미지 url을 받았을 때
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

    // 방에 입장했을 때
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

    // 방에 친구가 참여했을 때
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

    // 친구 초대 다이얼로그 열기
    fun openInviteDialog(friendDataList : ArrayList<FriendData>) {
        val dialog = InviteDialog(this)
        dialog.setOnOKClickedListener { content ->

            //선택된 친구가 있을 때
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

    // 친구 초대 post 요청
    private fun inviteRequest(friendsId: MutableSet<Long>) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postFriends(friendsId!!, myKakaoId!!).enqueue(object : Callback<SimpleResponseData> {

            override fun onResponse(call: Call<SimpleResponseData>, response: Response<SimpleResponseData>) {
                if (response.body()?.isSuccess!!) {
                    println("친구 초대 성공")
                }
            }

            override fun onFailure(call: Call<SimpleResponseData>, t: Throwable) {
                println("친구 초대 실패")
            }
        })
    }

    //인물필터 화면으로 이동하기
    fun gotoFaceFilter(position: Int) {
        var intent = Intent(applicationContext, SelectByPeopleActivity::class.java)
        intent.putExtra("friendId", joinFriendList[position].id)
        intent.putExtra("selectionIdList", selectionIdList)
        intent.putExtra("imageDataList", imageDataList)
        intent.putExtra("selected_profile_image", joinFriendList[position].picture.uri)
        setResult(1)
        activityResult.launch(intent)
    }

    // 로딩 다이얼로그 열기
    fun progressOn() {
        val binding = LoadingDialogBinding.inflate(layoutInflater)
        progressDialog = AppCompatDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.setContentView(binding.root)
        Glide.with(this).load(R.raw.loading_cat).into(binding.loadingImageview)
        progressDialog.show()
    }

    // 로딩 다이얼로그 닫기
    fun progressOff() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
    }

    // 나가기 다이얼로그 열기
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
//                    println("연결 끊기 실패.")
//                }
//                else {
//                    println("연결 끊기 성공. SDK에서 토큰 삭제됨")
//                }
//            }
            finish()
        }
        exitDialog.show()
    }

    // 초대 알림 다이얼로그 열기
    fun inviteDialogOn(id: Long?, roomIdx: Long?, picture: String?, nickname: String?) {
        val binding = InviteCheckDialogBinding.inflate(layoutInflater)
        inviteDialog = AppCompatDialog(this)
        inviteDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        Glide.with(this).load(picture).circleCrop().into(binding.friendPicture)
        binding.kakaoNickName.text = nickname
        binding.inviteRoomTitle.text = "${nickname}님이 초대를 보냈어요"
        binding.inviteRoomSubtitle.text = "${nickname}님의 방으로 입장할까요?"
        inviteDialog.setContentView(binding.root)
        inviteDialog.show()
        binding.inviteAcceptButton.setOnClickListener {
            // 내 id랑, 들어갈 방 번호 post 보내기
            postInviteResponse(id, roomIdx, picture, nickname)
            invite_pref.edit().clear().commit()
            inviteDialog.dismiss()
        }
        binding.inviteCancelButton.setOnClickListener {
            inviteDialog.dismiss()
            invite_pref.edit().clear().commit()
        }
    }

    // 친구 초대 수락 post 요청
    private fun postInviteResponse(id: Long?, roomIdx: Long?, picture: String?, nickname: String?) {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        // APIInterface 객체 생성
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
                println("초대 수락 실패")
            }
        })
    }

}



