package com.tumblers.picat

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.ActionBar
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

        // 액션바 제목 설정
        val actionbar: ActionBar? = supportActionBar
        actionbar?.title = "친구"

        val builder = AlertDialog.Builder(this, R.style.BasicDialogTheme)
        val createAlbumAlertView = LayoutInflater.from(this)
            .inflate(R.layout.create_album_alert_content, findViewById<ConstraintLayout>(R.id.create_ablum_alert_layout))

        builder.setView(createAlbumAlertView)
        var alertDialog : AlertDialog? = builder.create()
        alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // 이 멤버들로 공유 시작
        binding.startShareFab.setOnClickListener{
            // alert 띄우기
            alertDialog?.show()
        }

        // 앨범 생성 취소
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.cancel_create_album).setOnClickListener {
            alertDialog?.hide()
        }

        // 앨범 생성 확인
        createAlbumAlertView.findViewById<AppCompatButton>(R.id.confirm_create_album).setOnClickListener {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    // 나가기 버튼
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
