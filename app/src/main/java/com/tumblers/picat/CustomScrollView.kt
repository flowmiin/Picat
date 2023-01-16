package com.tumblers.picat

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class CustomScrollView: ScrollView {

    var SCROLL_UP = 0
    var SCROLL_DOWN = 1

    private var onScrollListener: OnScrollListener? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)
    constructor(context: Context, attr: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attr,
        defStyleAttr
    )

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        val direction = if (oldt > t) SCROLL_DOWN else SCROLL_UP

        onScrollListener?.onScroll(direction, t.toFloat())

    }

    fun setOnScrollListener(listener: OnScrollListener) {
        onScrollListener = listener
    }


    interface OnScrollListener {
        fun onScroll(direction: Int, scrollY: Float)
    }
}