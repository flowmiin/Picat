package com.tumblers.picat.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.ImageViewPagerActivity
import com.tumblers.picat.R
import com.tumblers.picat.dataclass.ImageData


class SelectPictureAdapter(
    private val mContext: Context,
    var mSelected: HashSet<Int>,
    var imageDataList: ArrayList<ImageData>
) :
    RecyclerView.Adapter<SelectPictureAdapter.TestViewHolder>() {
    private var mClickListener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): TestViewHolder {
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.item_picture, parent, false)
        return TestViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        Glide.with(mContext)
            .load(imageDataList[position].uri)
            .into(holder.imv)

        if (mSelected.contains(imageDataList[position].idx)) {
            holder.isSelectedButton.setImageResource(R.drawable.selected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.black_overlay)
        } else {
            holder.isSelectedButton.setImageResource(R.drawable.unselected_icn)
            holder.imv.foreground = mContext.getDrawable(R.color.transparent)
        }

        holder.zoomButton.setOnClickListener {
            var intent = Intent(mContext, ImageViewPagerActivity::class.java)
            intent.putExtra("imageList", imageDataList)
            intent.putExtra("current", position)
            mContext.startActivity(intent)
        }

        holder.isSelectedButton.setOnClickListener {
            toggleSelection(position)
        }
    }

    override fun getItemCount(): Int {
        return imageDataList.size
    }

    // ----------------------
    // Selection
    // ----------------------

    fun toggleSelection(pos: Int) {
        if (mSelected.contains(imageDataList[pos].idx)){
            mSelected.remove(imageDataList[pos].idx)
            println("pos: $pos idx: ${imageDataList[pos].idx}")
        } else {
            mSelected.add(imageDataList[pos].idx)
        }
        notifyItemChanged(pos)
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        for (i in start..end) {
            if (selected) {
                mSelected.add(imageDataList[i].idx)
            } else {
                mSelected.remove(imageDataList[i].idx)
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
        for (i in 0 until imageDataList.size) mSelected.add(i)
        notifyDataSetChanged()
    }

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
        var imv: ImageView = itemView.findViewById(R.id.imv)
        var isSelectedButton: ImageButton = itemView.findViewById(R.id.is_selected_imagebutton)
        var zoomButton: ImageButton = itemView.findViewById(R.id.zoom_imagebutton)

        init {
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