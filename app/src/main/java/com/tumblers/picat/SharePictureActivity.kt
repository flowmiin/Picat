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
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.sdk.friend.client.PickerClient
import com.kakao.sdk.friend.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.model.PickerOrientation
import com.kakao.sdk.friend.model.ViewAppearance
import com.kakao.sdk.user.UserApiClient
import com.tumblers.picat.adapter.BlurPictureAdapter
import com.tumblers.picat.adapter.PictureAdapter
import com.tumblers.picat.adapter.ProfilePictureAdapter
import com.tumblers.picat.adapter.SamePictureAdapter
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.dataclass.RequestInterface
import com.tumblers.picat.dataclass.ImageData
import com.tumblers.picat.fragment.DownloadCompleteFragment
import com.tumblers.picat.room.AppDatabase
import com.tumblers.picat.room.ImageTable
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
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var samePictureAdapter: SamePictureAdapter
    lateinit var blurPictureAdapter: BlurPictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter
    var startSelecting = false
    lateinit var mSocket: Socket
    lateinit var bottomSheetDialog: BottomSheetDialog


    var imageList: ArrayList<Uri> = ArrayList()
    var profileImageList: ArrayList<Uri> = ArrayList()
    val selectionList: MutableMap<String, Uri> = mutableMapOf<String, Uri>()

    var myKakaoId : Long? = null

//    private lateinit var db : AppDatabase
    private lateinit var imageDb : AppDatabase

    //뒤로가기 타이머
    var backKeyPressedTime: Long = 0

    // switch 상태 저장
    lateinit var pref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // db 초기화
//        db = AppDatabase.getInstance(applicationContext)!!
        imageDb = AppDatabase.getInstance(applicationContext)!!

//        val userId = db!!.idDao().get()
//        if (userId != null) {
//            myKakaoId = userId
//        }
//        else {
//            UserApiClient.instance.me { user, error ->
//                if (error != null) {
//                    Log.e(TAG, "사용자 정보 요청 실패", error)
//                } else if (user != null) {
//                    Log.e(TAG, "사용자 정보 요청 성공")
//                    myKakaoId = user.id
//                    db.idDao().insert(MyId(user.id!!))
//                }
//            }
//        }

        // 토큰 정보 보기
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            else if (tokenInfo != null) {
//                Toast.makeText(this, "토큰 정보 보기 성공", Toast.LENGTH_SHORT).show()
            }
        }
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.e(TAG, "사용자 정보 요청 성공")
                myKakaoId = user.id

                // socket 통신 연결
                mSocket = SocketApplication.get()
                mSocket.connect()
                mSocket.emit("join", myKakaoId)
                mSocket.on("image", onMessage)
                mSocket.on("join", onRoom)

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

        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // switch 버튼 체크 유무 저장
        pref = getPreferences(Context.MODE_PRIVATE)

        //임시 코드. 추후 기능 생성후 삭제예정
        val firstFace = binding.faceItemImageview
        val secFace = binding.faceItemImageview2
        val thirdFace = binding.faceItemImageview3
        firstFace.setOnClickListener {
            firstFace.isSelected = !firstFace.isSelected
            if(firstFace.isSelected){
                firstFace.background = this.getDrawable(R.color.picat_blue)
            }else{
                firstFace.background = this.getDrawable(R.color.picat_boundary_line_color)
            }
        }
        secFace.setOnClickListener {
            secFace.isSelected = !secFace.isSelected
            if(secFace.isSelected){
                secFace.background = this.getDrawable(R.color.picat_blue)
            }else{
                secFace.background = this.getDrawable(R.color.picat_boundary_line_color)
            }
        }
        thirdFace.setOnClickListener {
            thirdFace.isSelected = !thirdFace.isSelected
            if(thirdFace.isSelected){
                thirdFace.background = this.getDrawable(R.color.picat_blue)
            }else{
                thirdFace.background = this.getDrawable(R.color.picat_boundary_line_color)
            }
        }



        // 액션바 제목 설정
        val actionbar: ActionBar? = supportActionBar
        actionbar?.title = "공유방"

        // 프로필 사진 옆 플러스 버튼 = 수동으로 친구추가
        binding.profileItemPlusButton.setOnClickListener {
            val openPickerFriendRequestParams = OpenPickerFriendRequestParams(
                title = "풀 스크린 멀티 친구 피커", //default "친구 선택"
                viewAppearance = ViewAppearance.AUTO, //default ViewAppearance.AUTO
                orientation = PickerOrientation.AUTO, //default PickerOrientation.AUTO
                enableSearch = true, //default true
                enableIndex = true, //default true
                showMyProfile = true, //default true
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
                    }
                    // 친구 프로필을 화면에 띄우기
                    setProfileRecyclerview()
                }
            }
        }



        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList)
        samePictureAdapter = SamePictureAdapter(imageList, this)
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        profilePictureAdapter = ProfilePictureAdapter(profileImageList, this)

        // profile recyclerview 설정
        binding.sameRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.blurRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        setRecyclerView()


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
            // true 이면 false로, flase이면 true로 변경
            startSelecting = !startSelecting
            if(startSelecting){
                // 안내 토스트 띄우기
                Toast.makeText(this, "마음에 드는 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
            }else{
                // 안내 토스트 띄우기
                Toast.makeText(this, "선택 완료!", Toast.LENGTH_SHORT).show()
            }
            // 변경된 startSelecting 값에 따라 리사이클러뷰 어댑터 다시 설정
            setRecyclerView()

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
            bundle.putString("albumName", binding.roomNameEditText.text.toString())
            bundle.putString("firstPicture", imageList[0].toString())
            val downloadAlbumFragment = DownloadCompleteFragment()
            downloadAlbumFragment.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction().add(R.id.activity_share_picture_layout, downloadAlbumFragment)
            transaction.commit()
        }
    }

    // 포어그라운드 서비스에서 이미지 데이터 받는다.
//    override fun onNewIntent(intent: Intent?) {
//        if(intent != null) {
//            val newImageList : ArrayList<String>? = intent.getStringArrayListExtra("imageList")
//
//            for (image in newImageList!!) {
//                imageList.add(image.toUri())
//            }
//            setRecyclerView()
//        }
//        super.onNewIntent(intent)
//    }

    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.IO).launch {
            val getImage = imageDb!!.imageDao().getAll()
            if (getImage.isNotEmpty()) {
                for (i in getImage) {
                    if (!imageList.contains(i.imageUri.toUri())){
                        imageList.add(i.imageUri.toUri())
                    }
                }
                imageDb!!.imageDao().deleteAll()
                println("===OnResume===")
                setRecyclerView()
            }
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
            dir.mkdirs();
        }

        for (pic in selectionList){
            val imgUrl = pic.value
            println(imgUrl.toString())
            val fileName = getFileNameInUrl(imgUrl.toString())
            val localPath = "$savePath/$fileName"

            val imgDownloadCoroutine = CoroutineScope(Dispatchers.IO)
            imgDownloadCoroutine.launch {
                //url로 서버와 접속해서 이미지를 다운받기
                val conn: HttpURLConnection = URL(imgUrl.toString()).openConnection() as HttpURLConnection
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
            // 이미지가 선택된 경우
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
            // 단일 이미지 선택한 경우
//            else {
//                val imageUri = it.data!!.data
//
//                var file = File(getAbsolutePath(imageUri, this))
//                var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
//                var emitBody : MutableList<MultipartBody.Part>? = mutableListOf()
//                var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
//                emitBody?.add(body)
//                println("단일 이미지 선택")
//
//                apiRequest(emitBody, 1)
//            }
        }
    }

    fun getAbsolutePath(path: Uri?, context : Context): String {
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
            .baseUrl("http://43.200.93.112:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postImg(image_multipart!!, img_cnt, myKakaoId!!).enqueue(object : Callback<ImageData> {

            override fun onResponse(call: Call<ImageData>, response: Response<ImageData>) {
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

            override fun onFailure(call: Call<ImageData>, t: Throwable) {
                println("이미지 업로드 실패")
            }
        })
    }
    private fun setProfileRecyclerview(){
        // profile picture recyclerview 설정
        profilePictureAdapter = ProfilePictureAdapter(profileImageList, this)
        binding.profileRecyclerview.adapter = profilePictureAdapter
    }

    private fun setRecyclerView(){
        // 나머지 picture recyclerview 설정
        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList)
        binding.pictureRecyclerview.adapter = pictureAdapter

        // same recyclerview 설정
        samePictureAdapter = SamePictureAdapter(imageList, this)
        binding.sameRecyclerview.adapter = samePictureAdapter
        //중복 사진탭 열기
        binding.expandSameButton.setOnClickListener {
            if(binding.sameRecyclerview.visibility == View.VISIBLE) {
                binding.sameRecyclerview.visibility = View.GONE
                binding.expandSameButton.setImageResource(R.drawable.unfold_icn)
            }
            else {
                binding.sameRecyclerview.visibility = View.VISIBLE
                binding.expandSameButton.setImageResource(R.drawable.fold_icn)
            }
        }


        // blur recyclerview 설정
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        binding.blurRecyclerview.adapter = blurPictureAdapter
        // 흐린 사진탭 열기
        binding.expandBlurButton.setOnClickListener {
            if(binding.blurRecyclerview.visibility == View.VISIBLE) {
                binding.blurRecyclerview.visibility = View.GONE
                binding.expandBlurButton.setImageResource(R.drawable.unfold_icn)
            }
            else {
                binding.blurRecyclerview.visibility = View.VISIBLE
                binding.expandBlurButton.setImageResource(R.drawable.fold_icn)
            }
        }

    }

    fun refreshRecyclerView(){
        pictureAdapter.notifyDataSetChanged()
        samePictureAdapter.notifyDataSetChanged()
        blurPictureAdapter.notifyDataSetChanged()
        profilePictureAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    // 나가기 버튼
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
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



//    소켓통신 테스트
    var onMessage = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
                imageList.add(imgObj.toString().toUri())
            }
        }.invokeOnCompletion {
            setRecyclerView()
        }
    }

    var onRoom = Emitter.Listener { args->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
                if (!imageList.contains(imgObj.toString().toUri())){
                    imageList.add(imgObj.toString().toUri())
                }
            }
        }.invokeOnCompletion {
            setRecyclerView()
        }
    }
}

