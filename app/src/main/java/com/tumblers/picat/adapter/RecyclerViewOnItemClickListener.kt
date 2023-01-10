package com.tumblers.picat.adapter

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener


class RecyclerViewOnItemClickListener(
    context: Context?,
    recyclerView: RecyclerView,
    private val mListener: OnItemClickListener?
) :
    SimpleOnItemTouchListener() {
    private val mGestureDetector: GestureDetector

    init {
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {

            override fun onLongPress(e: MotionEvent) {
                val childView: View? = recyclerView.findChildViewUnder(e.x, e.y)
                if (childView != null && mListener != null) {
                    mListener.onItemLongClick(
                        childView,
                        recyclerView.getChildAdapterPosition(childView)
                    )
                }
            }
        })
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val child: View? = rv.findChildViewUnder(e.x, e.y)
        if (child != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
            mListener.onItemClick(child, rv.getChildAdapterPosition(child))
            return true
        }
        return false
    }

    interface OnItemClickListener {
        fun onItemClick(v: View?, position: Int)
//        fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean
        fun onItemLongClick(v: View?, position: Int)
    }
}