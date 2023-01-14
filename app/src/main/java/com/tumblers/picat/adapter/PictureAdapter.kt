package com.tumblers.picat.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.ImageViewPagerActivity
import com.tumblers.picat.R
import com.tumblers.picat.dataclass.ImageData

class PictureAdapter(private var imageList: ArrayList<ImageData>,
                     val mContext: Context,
                     var mSelected: HashSet<Int>)
    : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>(){
    private var mClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.item_picture, parent, false)

        return PictureViewHolder(view)
    }

    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        Glide.with(mContext)
            .load(imageList[position].uri)
            .into(holder.imv)
        
        holder.itemView.setOnClickListener {
            var intent = Intent(mContext, ImageViewPagerActivity::class.java)
            intent.putExtra("imageList", imageList)
            intent.putExtra("current", position)
            mContext.startActivity(intent)
        }

        // 화면 갱신 시 필요
        // 선택된 사진이면
        if (mSelected.contains(position)){
            holder.itemView.isSelected = true
            holder.isSelectedButton.setImageResource(R.drawable.selected_icn)
        }else{
            holder.isSelectedButton.visibility = View.INVISIBLE
        }
        holder.zoomButton.visibility = View.INVISIBLE

    }


    // 아이템 개수
    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    // ----------------------
    // Selection
    // ----------------------

    fun toggleSelection(view: View?, pos: Int) {
        val isSelectedButton = view?.findViewById<ImageButton>(R.id.is_selected_imagebutton)

        if (mSelected.contains(pos)) {
            view?.isSelected = false
            isSelectedButton?.setImageResource(R.drawable.unselected_icn)
            mSelected.remove(pos)
        } else {
            view?.isSelected = true
            isSelectedButton?.setImageResource(R.drawable.selected_icn)
            mSelected.add(pos)
        }
        notifyItemChanged(pos)
    }

    fun select(pos: Int, selected: Boolean) {
        if (selected) {
            mSelected.add(pos)
        } else {
            mSelected.remove(pos)
        }
        notifyItemChanged(pos)
    }


    fun selectRange(start: Int, end: Int, selected: Boolean) {
        for (i in start..end) {
            if (selected) {
                mSelected.add(i)
            } else{
                mSelected.add(i)
            }
        }
        notifyItemRangeChanged(start, end - start + 1)
    }

    fun deselectAll() {
        // this is not beautiful...
        mSelected.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        for (i in 0 until imageList.size) mSelected.add(i)
        notifyDataSetChanged()
    }

    fun getCountSelected(): Int {
        return mSelected.size
    }

    fun getSelection(): HashSet<Int> {
        return mSelected
    }

    // ----------------------
    // Click Listener
    // ----------------------

    fun setClickListener(itemClickListener: ItemClickListener) {
        mClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onItemLongClick(view: View?, position: Int): Boolean
    }

    // ----------------------
    // ViewHolder
    // ----------------------
    inner class PictureViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
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