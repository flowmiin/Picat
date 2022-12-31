package com.tumblers.picat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tumblers.picat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이 멤버들로 공유 시작
        binding.startShareFab.setOnClickListener{
            val intent = Intent(this, SharePictureActivity::class.java)
            startActivity(intent)
        }
    }
}