package com.tumblers.picat.adapter

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener

class DragSelectionItemTouchListener(context: Context?, listener: PictureAdapter.MyItemClickListener?) :
    LongPressItemTouchListener(context, listener), OnItemTouchListener {
    private var mPreviousViewHolder: RecyclerView.ViewHolder? = null
    private val mHitRect = Rect()
    private val mRangeSelection: MutableList<RecyclerView.ViewHolder?> = ArrayList()
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_POINTER_UP) {
            cancelPreviousSelection()
            return false
        } else {
            onLongPressedEvent(rv, e)
        }
        return mViewHolderLongPressed != null
    }

    private fun cancelPreviousSelection() {
        mViewHolderLongPressed = null
        mViewHolderInFocus = null
        mPreviousViewHolder = null
        mRangeSelection.clear()
    }


    private fun isMotionEventInCurrentViewHolder(e: MotionEvent): Boolean {
        if (mPreviousViewHolder != null) {
            mPreviousViewHolder!!.itemView.getHitRect(mHitRect)
            return mHitRect.contains(e.x.toInt(), e.y.toInt())
        }
        return false
    }



    private fun checkForSpanSelection(
        rv: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Boolean {
        if (rv.layoutManager is GridLayoutManager) {
            val endSelectionParams =
                viewHolder.itemView.layoutParams as GridLayoutManager.LayoutParams
            val startSelectionParams =
                mPreviousViewHolder!!.itemView.layoutParams as GridLayoutManager.LayoutParams
            if (endSelectionParams.spanIndex != startSelectionParams.spanIndex) {
                dispatchRangeSelection(rv, viewHolder)
                return true
            }
        }
        return false
    }

    private fun dispatchRangeSelection(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (mListener != null) {
            mRangeSelection.clear()
            val start =
                Math.min(mPreviousViewHolder!!.adapterPosition + 1, viewHolder.adapterPosition)
            val end =
                Math.max(mPreviousViewHolder!!.adapterPosition + 1, viewHolder.adapterPosition)
            for (i in start..end) {
                mRangeSelection.add(rv.findViewHolderForAdapterPosition(i))
            }
            mListener.onMultipleViewHoldersSelected(mRangeSelection)
        }
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_POINTER_UP) {
            cancelPreviousSelection()
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}