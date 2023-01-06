package com.tumblers.picat

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.sdk.user.UserApiClient
import com.tumblers.picat.adapter.BlurPictureAdapter
import com.tumblers.picat.adapter.PictureAdapter
import com.tumblers.picat.adapter.ProfilePictureAdapter
import com.tumblers.picat.adapter.SamePictureAdapter
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.dataclass.APIInterface
import com.tumblers.picat.dataclass.ImageData
import com.tumblers.picat.fragment.DownloadCompleteFragment
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    lateinit var samePictureAdapter: SamePictureAdapter
    lateinit var blurPictureAdapter: BlurPictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter
    var startSelecting = false
    lateinit var mSocket: Socket
    lateinit var bottomSheetDialog: BottomSheetDialog


    var imageList: ArrayList<Uri> = ArrayList()
    val selectionList: MutableMap<String, Uri> = mutableMapOf<String, Uri>()

    //뒤로가기 타이머
    var backKeyPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 토큰 정보 보기
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                Toast.makeText(this, "토큰 정보 보기 실패", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
            else if (tokenInfo != null) {
                Toast.makeText(this, "토큰 정보 보기 성공", Toast.LENGTH_SHORT).show()
            }
        }

        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(intent.type == "image/*") {
            if (intent.action == Intent.ACTION_SEND){
                val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                var file = File(getAbsolutePath(imageUri, this))
                var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                apiRequest(body)
            }
            else if(intent.action == Intent.ACTION_SEND_MULTIPLE){
                val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)

                val count: Int? = imageUriList?.toArray()?.size

                for (index in 0 until count!!) {
                    var file = File(getAbsolutePath(imageUriList[index], this))
                    var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                    var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    apiRequest(body)
                }
            }
        }

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




//        // mainActivity로부터 앨범이름 가져오기
//        val mainActivityIntent = intent
//        val roomName = mainActivityIntent.getStringExtra("albumName")
//        binding.roomNameTextview.text = roomName


        // 액션바 제목 설정
        val actionbar: ActionBar? = supportActionBar
        actionbar?.title = "공유방"

        // 프로필 사진 옆 플러스 버튼 = 수동으로 친구추가
        binding.profileItemPlusButton.setOnClickListener {
            val addFriendIntent = Intent(this, FriendListActivity::class.java)
            startActivity(addFriendIntent)
            // FriendListActivity에서 종료될 때 finish()를 호출하므로 여기서 호출하지 않습니다.
        }

        // socket 통신 연결
        mSocket = SocketApplication.get()
        mSocket.connect()
        mSocket.emit("join", "room1")
        mSocket.on("image", onMessage)
        mSocket.on("join", onRoom)


        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList)
        samePictureAdapter = SamePictureAdapter(imageList, this)
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        //profilePictureAdapter = ProfilePictureAdapter(imageList, this)

        // profile recyclerview 설정
        //binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.sameRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.blurRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)

        setRecyclerView()


        //바텀시트 초기화
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
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

        //바텀시트 내 업로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_upload_button).setOnClickListener {
            // permission 허용 확인
            val status = ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
            if (status == PackageManager.PERMISSION_GRANTED) {
                // 갤러리 호출
                val intent = Intent(Intent.ACTION_PICK)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                intent.type = "image/*"
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
            // TODO: 다운로드 실행
            imageDownload(binding.roomNameEditText.text.toString())


            val bundle = Bundle()
            bundle.putString("albumName", binding.roomNameEditText.text.toString())
            val downloadAlbumFragment = DownloadCompleteFragment()
            downloadAlbumFragment.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction().add(R.id.activity_share_picture_layout, downloadAlbumFragment)
            transaction.commit()
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
            // 다중 이미지 선택한 경우
            if(it.data!!.clipData != null) {
                val count = it.data!!.clipData!!.itemCount

                for (index in 0 until count) {
                    val imageUri = it.data!!.clipData!!.getItemAt(index).uri

//                    imageList.add(imageUri)

                    var file = File(getAbsolutePath(imageUri, this))
                    var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                    var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    apiRequest(body)

                }
            }
            // 단일 이미지 선택한 경우
            else {
                val imageUri = it.data!!.data
//                imageList.add(imageUri!!)
                var file = File(getAbsolutePath(imageUri, this))
                var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                apiRequest(body)
            }

//            setRecyclerView()

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


    private fun apiRequest(body: MultipartBody.Part) {
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("http://43.200.93.112:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: APIInterface = retrofit.create(APIInterface::class.java)
        server.postImg(body, "").enqueue(object : Callback<ImageData> {

            override fun onResponse(call: Call<ImageData>, response: Response<ImageData>) {
                println("이미지 업로드 ${response.isSuccessful}")
                if (response.isSuccessful){
//                    imageList.add()
                    println(response.body())
                    val jsonObject = JSONObject()
                    jsonObject.put("location", response.body()?.location)
                    mSocket.emit("image", jsonObject)
                }
            }

            override fun onFailure(call: Call<ImageData>, t: Throwable) {
                println("이미지 업로드 실패")
            }
        })
    }

    private fun setRecyclerView(){

        // profile recyclerview 설정
        //profilePictureAdapter = ProfilePictureAdapter(imageList, this)
        //binding.profileRecyclerview.adapter = profilePictureAdapter

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
                //카톡 로그인 사용자 동의 테스트용
//                UserApiClient.instance.unlink { error ->
//                    if (error != null) {
//                        println("연결 끊기 실패.")
//                    }
//                    else {
//                        println("연결 끊기 성공. SDK에서 토큰 삭제됨")
//                    }
//                }
                bottomSheetDialog.dismiss()
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
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
        Thread {
            runOnUiThread(Runnable {
                kotlin.run {
                    imageList.add(args[0].toString().toUri())
                    setRecyclerView()
                }
            })
        }.start()
    }

    var onRoom = Emitter.Listener { args->
        Thread {
            runOnUiThread(Runnable {
                kotlin.run {
                    val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
                    val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
                    for (i in 0..img_count - 1) {
                        val imgIObj = JSONObject(img_list[i].toString()).getString("url")
                        imageList.add(imgIObj.toString().toUri())
                    }
                    setRecyclerView()
                }
            })
        }.start()
    }
}

