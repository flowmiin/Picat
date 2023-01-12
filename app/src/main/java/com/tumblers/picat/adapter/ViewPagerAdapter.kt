package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R

class ViewPagerAdapter( val mContext: Context,
                        var imageList: ArrayList<Uri>)
                        : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PagerViewHolder((parent))

    override fun getItemCount(): Int = imageList.size

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        Glide.with(mContext)
            .load(imageList[position])
            .into(holder.pictureView)
    }

    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.view_pager_item, parent, false)){

        val pictureView = itemView.findViewById<ImageView>(R.id.viewPager_picture_imageview)
    }
}