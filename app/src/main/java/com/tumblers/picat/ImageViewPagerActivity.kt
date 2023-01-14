package com.tumblers.picat

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.tumblers.picat.adapter.ViewPagerAdapter
import com.tumblers.picat.databinding.ActivityImageViewPagerBinding
import com.tumblers.picat.dataclass.ImageData

class ImageViewPagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityImageViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var imageList = ArrayList<ImageData>()
        var current = 0
        if(intent.getSerializableExtra("imageList") != null){
            imageList = intent.getSerializableExtra("imageList") as ArrayList<ImageData>
            current = intent.getIntExtra("current", 0)
        }

        // 밑의 두줄의 순서를 유지할 것.
        binding.viewPager.adapter = ViewPagerAdapter(this, imageList)
        binding.viewPager.currentItem = current

    }
}