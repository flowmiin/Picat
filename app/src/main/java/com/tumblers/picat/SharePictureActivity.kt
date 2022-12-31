package com.tumblers.picat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.databinding.SharePictureBinding

class SharePictureActivity: AppCompatActivity(){
    lateinit var binding: SharePictureBinding

    lateinit var pictureAdapter: PictureAdapter
    lateinit var samePictureAdapter: SamePictureAdapter
    lateinit var blurPictureAdapter: BlurPictureAdapter

    var imageList: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SharePictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Adapter 초기화
        pictureAdapter = PictureAdapter(imageList, this)
        samePictureAdapter = SamePictureAdapter(imageList, this)
        blurPictureAdapter = BlurPictureAdapter(imageList, this)

        //recyclerview 설정
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

        // 업로드 버튼 이벤트
        binding.uploadButton.setOnClickListener {
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
            samePictureAdapter.notifyDataSetChanged()
            blurPictureAdapter.notifyDataSetChanged()

        }
    }
}

