package com.tumblers.picat

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.kakao.sdk.friend.client.PickerClient
import com.kakao.sdk.friend.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.model.PickerOrientation
import com.kakao.sdk.friend.model.ViewAppearance
import com.kakao.sdk.talk.TalkApiClient
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
//            // 카카오톡 친구 목록 가져오기 (기본)
//            TalkApiClient.instance.friends { friends, error ->
//                if (error != null) {
//                    Log.e(TAG, "카카오톡 친구 목록 가져오기 실패", error)
//                }
//                else if (friends != null) {
//                    Log.i(TAG, "카카오톡 친구 목록 가져오기 성공 \n${friends.elements?.joinToString("\n")}")
//                    // 친구의 UUID 로 메시지 보내기 가능
//                }
//            }
//            Toast.makeText(this, "친구 초대 완료!", Toast.LENGTH_SHORT).show()
//            finish()
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
