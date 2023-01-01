package com.tumblers.picat

import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tumblers.picat.databinding.ActivitySharePictureBinding

class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var samePictureAdapter: SamePictureAdapter
    lateinit var blurPictureAdapter: BlurPictureAdapter
    lateinit var profilePictureAdapter: ProfilePictureAdapter

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


        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this)
        samePictureAdapter = SamePictureAdapter(imageList, this)
        blurPictureAdapter = BlurPictureAdapter(imageList, this)
        profilePictureAdapter = ProfilePictureAdapter(imageList, this)

        // profile recyclerview 설정
        binding.profileRecyclerview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.profileRecyclerview.adapter = profilePictureAdapter

        // upload picture recyclerview 설정
        binding.pictureRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.pictureRecyclerview.adapter = pictureAdapter
        // GridView 형식으로 만들기
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)


        // same recyclerview 설정
        binding.sameRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.sameRecyclerview.adapter = samePictureAdapter
        binding.sameRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.expandSameButton.setOnClickListener {
            if(binding.sameRecyclerview.visibility == View.VISIBLE) {
                binding.sameRecyclerview.visibility = View.INVISIBLE
            }
            else {
                binding.sameRecyclerview.visibility = View.VISIBLE
            }
        }


        // blur recyclerview 설정
        binding.blurRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.blurRecyclerview.adapter = blurPictureAdapter
        binding.blurRecyclerview.layoutManager = GridLayoutManager(this, 3)
        binding.expandBlurButton.setOnClickListener {
            if(binding.blurRecyclerview.visibility == View.VISIBLE) {
                binding.blurRecyclerview.visibility = View.INVISIBLE
            }
            else {
                binding.blurRecyclerview.visibility = View.VISIBLE
            }
        }


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
            val intent = Intent()
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            // 다중 선택 기능
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
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

        // 다운로드 취소
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_alert).setOnClickListener {
            alertDialog?.dismiss()
        }

        // 다운로드 확인
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_alert).setOnClickListener {
            alertDialog?.dismiss()
            // TODO: 다운로드 실행
            // 다운로드 완료 후 안내 페이지로 이동
//            val intent = Intent(this, SharePictureActivity::class.java)
//            var newAlbumName = createAlbumAlertView.findViewById<EditText>(R.id.album_name_editText).text.toString()
//            // 비어있으면 기본값 주기
//            if (newAlbumName.isEmpty()){
//                newAlbumName = "새 앨범"
//            }
//
//            intent.putExtra("albumName", newAlbumName)
//            startActivity(intent)
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
            // 적용
            pictureAdapter.notifyDataSetChanged()
            samePictureAdapter.notifyDataSetChanged()
            blurPictureAdapter.notifyDataSetChanged()
            profilePictureAdapter.notifyDataSetChanged()

        }
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
}

