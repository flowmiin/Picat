package com.tumblers.picat

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PictureAdapter(): RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {
    lateinit var imageList: ArrayList<Uri>
    lateinit var context: Context

    constructor(imageList: ArrayList<Uri>, context: Context):this() {
        this.imageList = imageList
        this.context = context
    }

    // 화면 설정
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)

        val view: View = inflater.inflate(R.layout.item_picture, parent, false)

        return PictureViewHolder(view)
    }
    // 데이터 설정
    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        Glide.with(context)
            .load(imageList[position])
            .into(holder.uploadPicture)

    }
    // 아이템 개수
    override fun getItemCount(): Int {
        return imageList.size
    }

    class PictureViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val uploadPicture: ImageView = view.findViewById(R.id.upload_picture)
    }
}