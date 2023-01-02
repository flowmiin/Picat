package com.tumblers.picat

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tumblers.picat.adapter.BlurPictureAdapter
import com.tumblers.picat.adapter.PictureAdapter
import com.tumblers.picat.adapter.ProfilePictureAdapter
import com.tumblers.picat.adapter.SamePictureAdapter
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.fragment.DownloadCompleteFragment
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject

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


        // mainActivity로부터 앨범이름 가져오기
        val mainActivityIntent = intent
        val roomName = mainActivityIntent.getStringExtra("albumName")
        binding.roomName.text = roomName

        // 액션바 제목 설정
        val actionbar: ActionBar? = supportActionBar
        actionbar?.title = "공유방"


        // socket 통신 연결
        binding.switchButton.setOnCheckedChangeListener { p0, isChecked ->
            if (isChecked) {
                mSocket = SocketApplication.get()
                mSocket.connect()
                binding.sendButton.setOnClickListener {
                    mSocket.emit("message", binding.editText.text.toString())
                    Log.d("send socket", binding.editText.text.toString())
                }
                mSocket.on("message", onMessage)
            } else {
                mSocket.close()
            }
        }

        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList)
        samePictureAdapter = SamePictureAdapter(imageList, this)
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        profilePictureAdapter = ProfilePictureAdapter(imageList, this)

        // profile recyclerview 설정
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.sameRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.blurRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)
        setRecyclerView()



        //바텀시트 초기화
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val bottomSheetView = LayoutInflater.from(applicationContext)
            .inflate(R.layout.bottomsheet_content, findViewById<ConstraintLayout>(R.id.bottomsheet_layout))

        // fab버튼 클릭 시 바텀시트 활성화
        binding.openBottomsheetFab.setOnClickListener { view ->
            // bottomSheetDialog 뷰 생성, 호출
            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()
        }

        //바텀시트 내 업로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_upload_button).setOnClickListener {
            // 갤러리 호출
            val intent = Intent(Intent.ACTION_PICK)
            //intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            // 다중 선택 기능
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            //intent.action = Intent.ACTION_GET_CONTENT
            activityResult.launch(intent)
            bottomSheetDialog.hide()
        }

        // 다운로드 버튼 눌렀을 때 띄울 alert 생성
        val builder = AlertDialog.Builder(this, R.style.BasicDialogTheme)
        val createAlbumAlertView = LayoutInflater.from(this)
            .inflate(R.layout.basic_alert_content, findViewById<ConstraintLayout>(R.id.basic_alert_layout))
        createAlbumAlertView.findViewById<TextView>(R.id.alert_title).text = "'$roomName' ${getString(R.string.download_album_alert_title)}"
        createAlbumAlertView.findViewById<TextView>(R.id.alert_subtitle).text = getString(R.string.download_album_alert_subtitle)

        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = null

        //바텀시트 내 다운로드 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_download_button).setOnClickListener {
            bottomSheetDialog.hide()
            alertDialog = builder.create()
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()
        }

        //바텀시트 내 사진선택 버튼
        bottomSheetView.findViewById<ImageButton>(R.id.bottomsheet_select_button).setOnClickListener {
            bottomSheetDialog.hide()
            // true 이면 false로, flase이면 true로 변경
            startSelecting = !startSelecting
            // 변경된 startSelecting 값에 따라 리사이클러뷰 어댑터 다시 설정
            pictureAdapter = PictureAdapter(imageList, this, startSelecting, pictureAdapter.selectionList)
//            pictureAdapter.onItemSelectionChangedListener = {
//                Toast.makeText(applicationContext, "선택된 ID : $it", Toast.LENGTH_SHORT).show()
//            }
            binding.pictureRecyclerview.adapter = pictureAdapter

        }

        // 다운로드 취소
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_alert).setOnClickListener {
            alertDialog?.hide()
        }

        // 다운로드 확인
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_alert).setOnClickListener {
            alertDialog?.dismiss()
            // TODO: 다운로드 실행
            val transaction = supportFragmentManager.beginTransaction()
                .add(R.id.activity_share_picture_layout, DownloadCompleteFragment())
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
                // 선택한 이미지 개수
                val count = it.data!!.clipData!!.itemCount

                for (index in 0 until count) {
                    // 이미지 담기
                    val imageUri = it.data!!.clipData!!.getItemAt(index).uri
                    // 이미지 추가
                    imageList.add(imageUri)
                }
            }
            // 단일 이미지 선택한 경우
            else {
                val imageUri = it.data!!.data
                imageList.add(imageUri!!)
            }

//            refreshRecyclerView()
            setRecyclerView()



        }
    }

    private fun setRecyclerView(){

        // profile recyclerview 설정
        profilePictureAdapter = ProfilePictureAdapter(imageList, this)
        binding.profileRecyclerview.adapter = profilePictureAdapter

        // 나머지 picture recyclerview 설정
        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList)
//        pictureAdapter.onItemSelectionChangedListener = {
//            Toast.makeText(applicationContext, "선택된 ID : $it", Toast.LENGTH_SHORT).show()
//        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.out_button -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var onMessage = Emitter.Listener { args ->
        println("온메세지 진입")
        //val obj = JSONObject(args[0].toString())
        //println("온메세지 $obj")
        val a = binding.sendText.text.toString()
        Thread(object : Runnable {
            override fun run() {
                runOnUiThread(Runnable {
                    kotlin.run {
                        binding.sendText.text = args[0].toString()
                    }
                })
            }
        }).start()
    }
}

