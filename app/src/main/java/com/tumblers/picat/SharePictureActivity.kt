package com.tumblers.picat

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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


class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var samePictureAdapter: SamePictureAdapter
    lateinit var blurPictureAdapter: BlurPictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter
    val selectionList: MutableList<String> = mutableListOf<String>()
    var startSelecting = false


    lateinit var mSocket: Socket

    var imageList: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if((intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) && intent.type == "image/*") {
            println("전달 받은 사진: ${intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)}")
            val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)

            val count : Int? = imageUriList?.toArray()?.size

            for (index in 0 until count!!) {
                var file = File(getAbsolutePath(imageUriList[index], this))
                var requestFile = RequestBody.create(MediaType.parse("image/*"), file)
                var body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                apiRequest(body)
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
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
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
        var roomName = binding.roomNameEditText.text.toString()
        createAlbumAlertView.findViewById<TextView>(R.id.alert_title).text = "'$roomName' ${getString(R.string.download_album_alert_title)}"
        createAlbumAlertView.findViewById<TextView>(R.id.alert_subtitle).text = getString(R.string.download_album_alert_subtitle)
        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = builder.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        //바텀시트 내 다운로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_download_button).setOnClickListener {
            bottomSheetDialog.hide()
            alertDialog?.show()
        }

        //바텀시트 내 사진선택 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_select_button).setOnClickListener {
            bottomSheetDialog.hide()
            // true 이면 false로, flase이면 true로 변경
            startSelecting = !startSelecting
            if(startSelecting){
                // 안내 토스트 띄우기
                var toast = Toast.makeText(this, "마음에 드는 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
            }else{
                // 안내 토스트 띄우기
                Toast.makeText(this, "선택 완료!", Toast.LENGTH_SHORT).show()
            }
            // 변경된 startSelecting 값에 따라 리사이클러뷰 어댑터 다시 설정
            setRecyclerView()

        }

        // 다운로드 취소
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_alert).setOnClickListener {
            alertDialog?.hide()
        }

        // 다운로드 확인
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_alert).setOnClickListener {
            alertDialog?.dismiss()
            // TODO: 다운로드 실행
            val bundle = Bundle()
            bundle.putString("albumName", binding.roomNameEditText.text.toString())
            val downloadAlbumFragment = DownloadCompleteFragment()
            downloadAlbumFragment.arguments = bundle
            val transaction = supportFragmentManager.beginTransaction()
                .add(R.id.activity_share_picture_layout, downloadAlbumFragment)
            transaction.commit()
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
        binding.expandSameButton.setOnClickListener {
            if(binding.sameRecyclerview.visibility == View.VISIBLE) {
                binding.sameRecyclerview.visibility = View.GONE
            }
            else {
                binding.sameRecyclerview.visibility = View.VISIBLE
            }
        }


        // blur recyclerview 설정
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        binding.blurRecyclerview.adapter = blurPictureAdapter
        binding.expandBlurButton.setOnClickListener {
            if(binding.blurRecyclerview.visibility == View.VISIBLE) {
                binding.blurRecyclerview.visibility = View.GONE
            }
            else {
                binding.blurRecyclerview.visibility = View.VISIBLE
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
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
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

