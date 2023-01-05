package com.tumblers.picat

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
import com.tumblers.picat.databinding.ActivityFriendListBinding

class FriendListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityFriendListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 액션바 제목 설정
        val actionbar: ActionBar? = supportActionBar
        actionbar?.title = "친구"

        // 선택한 멤버들 추가하기
        binding.startShareFab.setOnClickListener{
            // TODO: 사용자가 선택한 친구들에게 초대링크 전송
            // 공유방으로 돌아가기
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
                // 여기서는 나가기가 아니라 카카오톡 친구 초대 api가 호출됩니다.
//                val intent = Intent(this, FriendListActivity::class.java)
//                startActivity(intent)
//                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
