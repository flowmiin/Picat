package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R

class PictureAdapter(private var imageList: ArrayList<Uri>,
                     val context: Context,
                     private var startSelecting: Boolean,
                     private val selectionList: MutableMap<String, Uri>,
                     var selectionIdList: HashSet<Int>)
    : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {
    private var mClickListener: ItemClickListener? = null


    var onItemSelectionChangedListener : ((MutableMap<String, Uri>) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.item_picture, parent, false)

        return PictureViewHolder(view)
    }

    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        val id = getItemId(position).toString()
        holder.itemView.tag = id
        Glide.with(context)
            .load(imageList[position])
            .into(holder.itemView.findViewById(R.id.iv_image))

        val isSelectedButton = holder.itemView.findViewById<ImageButton>(R.id.is_selected_imagebutton)
        val zoomButton = holder.itemView.findViewById<ImageButton>(R.id.zoom_imagebutton)

        // 화면 갱신 시 필요
        // 선택된 사진이면
        if (selectionList.contains(id)){
            holder.itemView.isSelected = true
            isSelectedButton.setImageResource(R.drawable.selected_icn)
            isSelectedButton.visibility = View.VISIBLE
        }

        // 선택중일때
        if (startSelecting){
            holder.itemView.isClickable = true
            isSelectedButton.visibility = View.VISIBLE
            zoomButton.visibility = View.VISIBLE
        }
        else{
            // 선택 완료했을 때
            holder.itemView.isClickable = false
            isSelectedButton.visibility = View.INVISIBLE
            zoomButton.visibility = View.INVISIBLE
        }


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

        if (selectionIdList.contains(pos)) {
            view?.isSelected = false
            isSelectedButton?.setImageResource(R.drawable.unselected_icn)
            selectionList.remove(pos.toString())
            selectionIdList.remove(pos)
        } else {
            view?.isSelected = true
            isSelectedButton?.setImageResource(R.drawable.selected_icn)
            selectionList[pos.toString()] = imageList[pos]
            selectionIdList.add(pos)
        }
        notifyItemChanged(pos)
    }

    fun select(pos: Int, selected: Boolean) {
        if (selected) {
            selectionList[pos.toString()] = imageList[pos]
            selectionIdList.add(pos)
        } else {
            selectionList.remove(pos.toString())
            selectionIdList.remove(pos)
        }
        notifyItemChanged(pos)
    }

    fun selectRange(start: Int, end: Int, selected: Boolean) {
        for (i in start..end) {
            if (selected) {
                selectionList[i.toString()] = imageList[i]
                selectionIdList.add(i)
            } else{
                selectionList.remove(i.toString())
                selectionIdList.add(i)
            }
        }
//        onItemSelectionChangedListener?.let { it(selectionList) }
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
        return selectionList.size
    }

    fun getSelection(): HashSet<Int> {
        return selectionIdList
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
    inner class PictureViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {

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