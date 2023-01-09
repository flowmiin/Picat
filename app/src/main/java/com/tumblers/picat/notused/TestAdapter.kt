package com.tumblers.picat.notused

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tumblers.picat.R


class TestAdapter(
    private val mContext: Context,
    private val mDataSize: Int,
    var mSelected: HashSet<Int?>
) :
    RecyclerView.Adapter<TestAdapter.TestViewHolder>() {
    private var mClickListener: ItemClickListener? = null

    init {
        mSelected = HashSet()
    }

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): TestViewHolder {
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.item_test, parent, false)
        return TestViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
        holder.tvText.text = position.toString()
        if (mSelected.contains(position)) {
            holder.tvText.setBackgroundColor(Color.RED)
        } else {
            holder.tvText.setBackgroundColor(Color.WHITE)
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
        var tvText: TextView

        init {
            tvText = itemView.findViewById<View>(R.id.tvText) as TextView
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