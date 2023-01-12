package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R

class InviteFriendPictureAdapter (private var imageList: ArrayList<Uri>,
                                  val mContext: Context,
                                  var mSelected: HashSet<Int>)
    : RecyclerView.Adapter<InviteFriendPictureAdapter.InviteFriendPictureViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteFriendPictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.item_picture, parent, false)

        return InviteFriendPictureViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: InviteFriendPictureViewHolder, position: Int) {
        Glide.with(mContext)
            .load(imageList[position])
            .into(holder.imv)

        if (mSelected.contains(position)) {
            holder.isSelectButton.setImageResource(R.drawable.selected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.black_overlay)
        }
        else {
            holder.isSelectButton.setImageResource(R.drawable.unselected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.transparent)
        }
        holder.zoomButton.visibility = View.INVISIBLE

        holder.isSelectButton.setOnClickListener {
            toggleSelection(position)
        }

    }

    override fun getItemCount(): Int {
        return imageList.size
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
        var imv: ImageView = view.findViewById(R.id.imv)
        var isSelectButton: ImageButton = view.findViewById(R.id.is_selected_imagebutton)
        var zoomButton: ImageButton = view.findViewById(R.id.zoom_imagebutton)

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