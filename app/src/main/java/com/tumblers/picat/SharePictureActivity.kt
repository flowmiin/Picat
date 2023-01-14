package com.tumblers.picat

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.friend.client.PickerClient
import com.kakao.sdk.friend.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.model.PickerOrientation
import com.kakao.sdk.friend.model.ViewAppearance
import com.kakao.sdk.user.UserApiClient
import com.tumblers.picat.adapter.*
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.dataclass.ImageData
import com.tumblers.picat.dataclass.ImageResponseData
import com.tumblers.picat.dataclass.RequestInterface
import com.tumblers.picat.fragment.DownloadCompleteFragment
import com.tumblers.picat.service.MyFirebaseMessagingService
import com.tumblers.picat.service.ForegroundService
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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


class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter
    
    var mSocket: Socket? = null
    lateinit var bottomSheetDialog: BottomSheetDialog

    var profileImageList: ArrayList<Uri> = ArrayList()
    var profileKakaoIdList: ArrayList<Long?> = ArrayList()

    var imageList: ArrayList<Uri> = ArrayList()
    var imageDataList: ArrayList<ImageData> = ArrayList()
    val selectionIdList: HashSet<Int> = hashSetOf()

    // 유저 데이터
    var myKakaoId : Long? = null
    var myNickname: String? = ""
    var myPicture: String? = ""
    var myEmail: String? = ""

    //뒤로가기 타이머
    var backKeyPressedTime: Long = 0

    // switch 상태 저장
    lateinit var pref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        
        // 토큰 정보 확인 후
        // 실패 시 로그인 화면으로 이동
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            else if (tokenInfo != null) {
            }
        }

        // switch 버튼 체크 유무 저장
        pref = getPreferences(Context.MODE_PRIVATE)

        // 사용자 정보 요청 및 소켓 연결
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.e(TAG, "사용자 정보 요청 성공")

                myKakaoId = user.id
                myEmail = user.kakaoAccount?.email
                myPicture = user.kakaoAccount?.profile?.profileImageUrl
                myNickname = user.kakaoAccount?.profile?.nickname

                profileImageList.add(myPicture.toString().toUri())
                profileKakaoIdList.add(myKakaoId)
                setProfileRecyclerview()


                // socket 통신 연결
                mSocket = SocketApplication.get()
                mSocket?.connect()
                mSocket?.emit("join", myKakaoId)
                mSocket?.on("image", onMessage)
                mSocket?.on("join", onRoom)
                mSocket?.on("friends", onFriends)

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

        // FCM 알림 받기
        MyFirebaseMessagingService().getFirebaseToken()
        FirebaseMessaging.getInstance().token.addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                println("my token = ${task.result}")
            }
        }


        // 액션바 제목 설정
        val actionbar = binding.toolbar
        setSupportActionBar(actionbar) //커스텀한 toolbar를 액션바로 사용
        supportActionBar?.setDisplayShowTitleEnabled(false) //액션바에 표시되는 제목의 표시유무를 설정합니다. false로 해야 custom한 툴바의 이름이 화면에 보이게 됩니다.
        actionbar.title = "공유방"

        // 프로필 사진 옆 플러스 버튼: 카카오 친구 피커 실행
        // 수동으로 친구추가
        binding.profileItemPlusButton.setOnClickListener {
            val openPickerFriendRequestParams = OpenPickerFriendRequestParams(
                title = "풀 스크린 멀티 친구 피커", //default "친구 선택"
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
                    for (i in 0..friend_count!! - 1) {
                        profileImageList.add(selectedUsers?.users?.get(i)?.profileThumbnailImage.toString().toUri())
                        profileKakaoIdList.add(selectedUsers?.users?.get(i)?.id)
                    }
                    // 친구 프로필을 화면에 띄우기
                    setProfileRecyclerview()
                }
            }
        }

        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageDataList, this, selectionIdList)
        profilePictureAdapter = ProfilePictureAdapter(profileImageList, profileKakaoIdList, selectionIdList, this)

        //recyclerview 레이아웃 설정
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        binding.pictureRecyclerview.adapter = pictureAdapter

        binding.pictureRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, 10, includeEdge = false))




        //바텀시트 초기화
        bottomSheetDialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialog)
        val bottomSheetView = LayoutInflater.from(applicationContext)
            .inflate(R.layout.bottomsheet_content, findViewById<ConstraintLayout>(R.id.bottomsheet_layout))

        bottomSheetDialog.setContentView(bottomSheetView)

        // fab버튼 클릭 시 바텀시트 활성화
        binding.openBottomsheetFab.setOnClickListener {
            bottomSheetDialog.show()
        }

        // permission 허용 요청
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

        // 자동 업로드 스위치 버튼 저장값 불러오기
        if(pref.getBoolean("store_check", false)) {
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
                    pref.edit().putBoolean("store_check", true).apply()
                    ContextCompat.startForegroundService(this, serviceIntent)
                    Toast.makeText(this, "Foreground Service start", Toast.LENGTH_SHORT).show()
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
                pref.edit().putBoolean("store_check", false).apply()
                Toast.makeText(this, "Foreground Service stop", Toast.LENGTH_SHORT).show()
            }
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
            bottomSheetDialog.hide()

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
            bundle.putString("albumCover", imageDataList[pictureAdapter.mSelected.first()].uri.toString())
            val downloadAlbumFragment = DownloadCompleteFragment()
            downloadAlbumFragment.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction().add(R.id.activity_share_picture_layout, downloadAlbumFragment)
            transaction.commit()
        }
    }
    

    override fun onResume() {
        super.onResume()

        // 자동업로드 설정하고 다른 화면 갔다가 다시 돌아왔을 때
        // 소켓 연결이 끊어졌을 떄
        // socket 통신 연결
        if(mSocket == null){
            mSocket = SocketApplication.get()
            mSocket?.connect()
            mSocket?.emit("join", myKakaoId)
            mSocket?.on("image", onMessage)
            mSocket?.on("join", onRoom)
        }
    }

    fun getFileNameInUrl(imgUrl: String): String{
        val ary = imgUrl.split("/")
        println(ary)
        return ary[3]
    }

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
                var tmp = it.data!!.getSerializableExtra("selectonIdList")!! as HashSet<Int>
                println("추가로 선택된 이미지: ${tmp}")
                selectionIdList.clear()
                for (i in tmp){
                    pictureAdapter.select(i, true)
                    selectionIdList.add(i)
                }
//                binding.pictureRecyclerview.adapter?.notifyDataSetChanged()
                setRecyclerView()
            }
        }
    }

    private fun getAbsolutePath(path: Uri?, context : Context): String {
        var proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        var c: Cursor? = context.contentResolver.query(path!!, proj, null, null, null)
        var index = c?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        c?.moveToFirst()

        var result = c?.getString(index!!)

        return result!!
    }


    private fun apiRequest(image_multipart: MutableList<MultipartBody.Part>?, img_cnt: Int) {
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postImg(image_multipart!!, img_cnt, myKakaoId!!).enqueue(object : Callback<ImageResponseData> {

            override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                println("이미지 업로드 ${response.isSuccessful}")
                if (response.isSuccessful){
                    println("이미지 response ${response.body()}")
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

                    mSocket?.emit("image", jsonObject)
                }
            }

            override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                println("이미지 업로드 실패")
            }
        })
    }

    private fun setProfileRecyclerview(){
        // profile picture recyclerview 설정
        profilePictureAdapter = ProfilePictureAdapter(profileImageList, profileKakaoIdList, selectionIdList, this)
        binding.profileRecyclerview.adapter = profilePictureAdapter
    }

    private fun setRecyclerView(){
        // picture recyclerview 설정
        pictureAdapter = PictureAdapter(imageDataList, this, selectionIdList)
        binding.pictureRecyclerview.adapter = pictureAdapter


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            // 나가기 버튼
            R.id.out_button -> {
                //카톡 로그인 사용자 동의 및 회원가입 테스트용
                UserApiClient.instance.unlink { error ->
                    if (error != null) {
                        println("연결 끊기 실패.")
                    }
                    else {
                        println("연결 끊기 성공. SDK에서 토큰 삭제됨")
                    }
                }
                bottomSheetDialog.dismiss()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "뒤로 버튼을 한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            bottomSheetDialog.dismiss()
            finish()
        }
    }


    var onMessage = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
                val imgData = ImageData(imageDataList.size, imgObj.toString().toUri())
                imageDataList.add(imgData)
            }
            setRecyclerView()
        }
    }

    var onRoom = Emitter.Listener { args->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
//                if (!imageList.contains(imgObj.toString().toUri())){
//                    imageList.add(imgObj.toString().toUri())
//                }
                val imgData = ImageData(imageDataList.size, imgObj.toString().toUri())
                imageDataList.add(imgData)
                // 데이터 중복 제거
                imageDataList.distinct()
            }
            setRecyclerView()
        }
    }

    var onFriends = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
//                if (!imageList.contains(imgObj.toString().toUri())) {
//                    imageList.add(imgObj.toString().toUri())
//                }
                val imgData = ImageData(imageDataList.size, imgObj.toString().toUri())
                imageDataList.add(imgData)
                // 데이터 중복 제거
                imageDataList.distinct()
            }
            // setRecyclerView()
        }
    }
}



