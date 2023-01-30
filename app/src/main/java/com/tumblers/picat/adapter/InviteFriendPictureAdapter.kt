package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R
import com.tumblers.picat.dataclass.FriendData
import com.tumblers.picat.dataclass.ImageData

class InviteFriendPictureAdapter (var friendDataList : ArrayList<FriendData>,
                                  val mContext: Context,
                                  var mSelected: HashSet<Int>)
    : RecyclerView.Adapter<InviteFriendPictureAdapter.InviteFriendPictureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteFriendPictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.invite_item_picture, parent, false)

        return InviteFriendPictureViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: InviteFriendPictureViewHolder, position: Int) {
        Glide.with(mContext)
            .load(friendDataList[position].picture.uri)
            .circleCrop()
            .into(holder.imv)

        holder.name.text = friendDataList[position].nickName

        if (mSelected.contains(position)) {
            holder.border.background = mContext.getDrawable(R.drawable.check_profile_background)
            holder.imv.foreground = mContext.getDrawable(R.drawable.check_icn)
        }
        else {
            holder.imv.foreground = null
            holder.border.background = mContext.getDrawable(R.drawable.not_check_profile_background)
        }

        holder.imv.setOnClickListener {
            toggleSelection(position)
        }

    }

    override fun getItemCount(): Int {
        return friendDataList.size
    }

    fun select(pos: Int, selected: Boolean) {
        if (selected) {
            mSelected.add(pos)
        } else {
            mSelected.remove(pos)
        }
        notifyItemChanged(pos)
    }

    fun toggleSelection(pos: Int) {
        if (mSelected.contains(pos)) {
            mSelected.remove(pos)
        } else {
            mSelected.add(pos)
        }
        notifyItemChanged(pos)
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    private var mClickListener: OnItemClickListener? = null

    fun setClickListener(itemClickListener: OnItemClickListener?) {
        mClickListener = itemClickListener
    }


    inner class InviteFriendPictureViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        var imv: ImageView = view.findViewById(R.id.invite_profile_picture)
        var border: ConstraintLayout = view.findViewById(R.id.invite_item_constraintlayout)
//        var isSelectButton: ImageButton = view.findViewById(R.id.is_selected_imagebutton)
//        var zoomButton: ImageButton = view.findViewById(R.id.zoom_imagebutton)
        var name: TextView = view.findViewById(R.id.kakao_nick_name)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (mClickListener != null) {
                mClickListener!!.onItemClick(view, bindingAdapterPosition)
            }
        }

    }
}