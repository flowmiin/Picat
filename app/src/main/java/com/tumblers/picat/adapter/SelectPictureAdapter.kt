package com.tumblers.picat.adapter

import android.content.Context
import android.media.Image
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R


class SelectPictureAdapter(
    private val mContext: Context,
    private val mDataSize: Int,
    var mSelected: HashSet<Int>,
    var imageList: ArrayList<Uri>
) :
    RecyclerView.Adapter<SelectPictureAdapter.TestViewHolder>() {
    private var mClickListener: ItemClickListener? = null

//    init {
//        mSelected = HashSet()
//    }

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): TestViewHolder {
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.item_picture, parent, false)
        return TestViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        Glide.with(mContext)
            .load(imageList[position])
            .into(holder.imv)

        if (mSelected.contains(position)) {
            holder.isSelectedButton.setImageResource(R.drawable.selected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.black_overlay)
        } else {
            holder.isSelectedButton.setImageResource(R.drawable.unselected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.transparent)
        }

        holder.zoomButton.setOnClickListener {
            //확대하기
            Toast.makeText(mContext, "확대하기", Toast.LENGTH_SHORT).show()
        }

        holder.isSelectedButton.setOnClickListener {
            toggleSelection(position)
        }
    }

    override fun getItemCount(): Int {
        return mDataSize
    }

    // ----------------------
    // Selection
    // ----------------------

    fun toggleSelection(pos: Int) {
        if (mSelected.contains(pos)) mSelected.remove(pos) else mSelected.add(pos)
        notifyItemChanged(pos)
    }

    fun select(pos: Int, selected: Boolean) {
        if (selected) mSelected.add(pos) else mSelected.remove(pos)
        notifyItemChanged(pos)
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        for (i in start..end) {
            if (selected) mSelected.add(i) else mSelected.remove(i)
        }
        notifyItemRangeChanged(start, end - start + 1)
    }

//    fun deselectAll() {
//        // this is not beautiful...
//        mSelected.clear()
//        notifyDataSetChanged()
//    }

//    fun selectAll() {
//        for (i in 0 until mDataSize) mSelected.add(i)
//        notifyDataSetChanged()
//    }

    fun getCountSelected(): Int {
        return mSelected.size
    }

    // ----------------------
    // Click Listener
    // ----------------------

    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onItemLongClick(view: View?, position: Int): Boolean
    }

    // ----------------------
    // ViewHolder
    // ----------------------

    inner class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, OnLongClickListener {
        var imv: ImageView
        var isSelectedButton: ImageButton
        var zoomButton: ImageButton

        init {
            imv = itemView.findViewById(R.id.imv)
            isSelectedButton = itemView.findViewById(R.id.is_selected_imagebutton)
            zoomButton = itemView.findViewById(R.id.zoom_imagebutton)
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, bindingAdapterPosition)
        }

        override fun onLongClick(view: View): Boolean {
            return if (mClickListener != null) mClickListener!!.onItemLongClick(
                view,
                bindingAdapterPosition
            ) else false
        }
    }
}