package com.tumblers.picat

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tumblers.picat.adapter.InviteFriendPictureAdapter
import com.tumblers.picat.databinding.FriendInviteDialogBinding
import com.tumblers.picat.dataclass.FriendData
import com.tumblers.picat.dataclass.ImageData
import kotlinx.coroutines.selects.select
import java.util.prefs.Preferences

class InviteDialog(private val context : AppCompatActivity) {

    private lateinit var binding: FriendInviteDialogBinding
    private val dialog = Dialog(context)

    private lateinit var listener : InviteDialogClickedListener

    lateinit var inviteFriendPictureAdapter: InviteFriendPictureAdapter

    var selectionIdList: HashSet<Int> = hashSetOf()



    fun show(friendDataList : ArrayList<FriendData>) {
        binding = FriendInviteDialogBinding.inflate(context.layoutInflater)

        dialog.requestWindowFeature((Window.FEATURE_ACTION_BAR)) // 액션 바 제거
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT)
//        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false) // 다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        // 확인 버튼
        binding.inviteCheckButton.setOnClickListener {
            // 선택한 친구들이 있을 때
            if (inviteFriendPictureAdapter.mSelected.isNotEmpty()){
                listener.onClicked(inviteFriendPictureAdapter.mSelected)
                dialog.dismiss()
            }else{
                // 선택한 친구들이 없을 때
                dialog.findViewById<TextView>(R.id.invite_warning_text).visibility = View.VISIBLE
                dialog.findViewById<TextView>(R.id.invite_text).visibility = View.GONE
            }

        }
        // 취소 버튼
        binding.cancelCheckButton.setOnClickListener{
            dialog.dismiss()
        }

        inviteFriendPictureAdapter = InviteFriendPictureAdapter(friendDataList, context, selectionIdList)
        binding.inviteRecyclerview.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.inviteRecyclerview.adapter = inviteFriendPictureAdapter

        inviteFriendPictureAdapter.setClickListener(object : InviteFriendPictureAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                inviteFriendPictureAdapter.toggleSelection(position)
            }
        })


        dialog.show()
    }

    interface InviteDialogClickedListener {
        fun onClicked(content : HashSet<Int>)
    }

    fun setOnOKClickedListener(listener: (HashSet<Int>) -> Unit) {
        this.listener = object: InviteDialogClickedListener {
            override fun onClicked(content: HashSet<Int>) {
                listener(content)
            }
        }
    }
}