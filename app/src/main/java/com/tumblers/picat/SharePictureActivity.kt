package com.tumblers.picat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tumblers.picat.databinding.ActivitySharePictureBinding

class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: ActivitySharePictureBinding

    lateinit var pictureAdapter: PictureAdapter

    var imageList: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this)

        //recyclerview 설정
        binding.pictureRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.pictureRecyclerview.adapter = pictureAdapter
        // GridView 형식으로 만들기
        binding.pictureRecyclerview.layoutManager = GridLayoutManager(this, 3)

        //바텀시트 초기화
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = LayoutInflater.from(applicationContext)
            .inflate(R.layout.bottomsheet_content, findViewById(R.id.bottomsheet_layout) as ConstraintLayout?)

        // fab버튼 클릭 시 바텀시트 활성화
        binding.openBottomsheetFab.setOnClickListener { view ->
            // bottomSheetDialog 뷰 생성
            bottomSheetDialog.setContentView(bottomSheetView)
            // bottomSheetDialog 호출
            bottomSheetDialog.show()
        }

        //바텀시트 내 업로드 버튼
        bottomSheetView.findViewById<Button>(R.id.bottomsheet_upload_button).setOnClickListener {
            // 갤러리 호출
            val intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            //intent.type = "image/*"
            // 다중 선택 기능
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            activityResult.launch(intent)
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
        }
    }
}

