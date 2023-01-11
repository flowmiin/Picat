package com.tumblers.picat.notused

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.tumblers.picat.adapter.PictureAdapter


abstract class LongPressItemTouchListener(context: Context?, listener: PictureAdapter.OnItemInteractionListener?) :
    SimpleOnItemTouchListener() {
    private val mGestureDetector: GestureDetector
    protected val mListener: PictureAdapter.OnItemInteractionListener?
    protected var mViewHolderLongPressed: RecyclerView.ViewHolder? = null
    var mViewHolderInFocus: RecyclerView.ViewHolder? = null

    init {
        mGestureDetector = GestureDetector(context, LongPressGestureListener())
        mGestureDetector.setIsLongpressEnabled(true)
        mListener = listener
    }

    fun onLongPressedEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (mViewHolderLongPressed != null) {
            return false
            // long pressed happened, my job here is done.
        }
        val childViewUnder = rv.findChildViewUnder(e.x, e.y)
        if (childViewUnder != null) {
            mViewHolderInFocus = rv.findContainingViewHolder(childViewUnder)
            if (mGestureDetector.onTouchEvent(e) && mListener != null) {
                mListener.onItemClicked(
                    rv,
                    mViewHolderInFocus,
                    rv.getChildAdapterPosition(childViewUnder)
                )
            }
            return mViewHolderLongPressed != null
        }
        return false
    }

    internal inner class LongPressGestureListener : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (mViewHolderInFocus != null && mListener != null) {
                val recyclerView = mViewHolderInFocus!!.itemView.parent as RecyclerView
                mListener.onLongItemClicked(
                    recyclerView,
                    mViewHolderInFocus,
                    mViewHolderInFocus!!.bindingAdapterPosition
                )
                mViewHolderLongPressed = mViewHolderInFocus
            }
        }
    }
}