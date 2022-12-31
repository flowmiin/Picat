package com.tumblers.picat

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.tumblers.picat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val builder = AlertDialog.Builder(this, R.style.CreateAlbumDialogTheme)
        val createAlbumAlertView = LayoutInflater.from(this)
            .inflate(R.layout.create_album_alert_content, findViewById<ConstraintLayout>(R.id.create_ablum_alert_layout))

        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = null

        // 이 멤버들로 공유 시작
        binding.startShareFab.setOnClickListener{
            // alert 띄우기
            alertDialog = builder.create()
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()
        }

        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_create_album).setOnClickListener {
            // 앨범 생성 취소
            alertDialog?.dismiss()
        }

        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_create_album).setOnClickListener {
            // 앨범 생성 확인
            alertDialog?.dismiss()
            val intent = Intent(this, SharePictureActivity::class.java)
            var newAlbumName = createAlbumAlertView.findViewById<EditText>(R.id.album_name_editText).text.toString()
            // 비어있으면 기본값 주기
            if (newAlbumName.isEmpty()){
                newAlbumName = "새 앨범"
            }

            intent.putExtra("albumName", newAlbumName)
            startActivity(intent)
            finish()
        }
    }
}
