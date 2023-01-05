package com.tumblers.picat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import com.tumblers.picat.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionbar: ActionBar? = supportActionBar
        actionbar?.hide()

        // 카카오톡으로 로그인
        binding.kakaoLoginButton.setOnClickListener{
            // TODO: 추후 로그인 feature 추가해야합니다.
            // 현재는 그냥 공유방 화면 전환
            val intent = Intent(this, SharePictureActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}