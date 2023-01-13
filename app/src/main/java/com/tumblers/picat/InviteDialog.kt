package com.tumblers.picat

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tumblers.picat.adapter.InviteFriendPictureAdapter
import com.tumblers.picat.databinding.FriendInviteDialogBinding
import kotlinx.coroutines.selects.select

class InviteDialog(private val context : AppCompatActivity) {

    private lateinit var binding: FriendInviteDialogBinding
    private val dialog = Dialog(context)

    private lateinit var listener : InviteDialogClickedListener

    lateinit var inviteFriendPictureAdapter: InviteFriendPictureAdapter

    var selectionIdList: HashSet<Int> = hashSetOf()

    fun show(imageList : ArrayList<Uri>, nameList: ArrayList<String>) {
        binding = FriendInviteDialogBinding.inflate(context.layoutInflater)

        dialog.requestWindowFeature((Window.FEATURE_ACTION_BAR)) // 액션 바 제거
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.setCancelable(false) // 다이얼로그으 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        binding.inviteCheckButton.setOnClickListener {
            listener.onClicked(inviteFriendPictureAdapter.mSelected)
            dialog.dismiss()
        }
        binding.cancelCheckButton.setOnClickListener{
            dialog.dismiss()
        }

        inviteFriendPictureAdapter = InviteFriendPictureAdapter(imageList, nameList, context, selectionIdList)
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