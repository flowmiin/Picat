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
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
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
        binding.roomName.text = mainActivityIntent.getStringExtra("albumName")
//        val tmpToast = Toast.makeText(this, mainActivityIntent.getStringExtra("albumName"), Toast.LENGTH_SHORT)
//        tmpToast.show()


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
                binding.sameRecyclerview.visibility = View.GONE
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
                binding.blurRecyclerview.visibility = View.GONE
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
            R.id.out_button -> { finish() }
        }
        return super.onOptionsItemSelected(item)
    }
}

