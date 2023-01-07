package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R

class ProfilePictureAdapter(): RecyclerView.Adapter<ProfilePictureAdapter.ProfilePictureViewHolder>() {
    lateinit var imageList: ArrayList<Uri>
    lateinit var context: Context

    constructor(imageList: ArrayList<Uri>, context: Context):this() {
        this.imageList = imageList
        this.context = context
    }

    // 화면 설정
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)

        val view: View = inflater.inflate(R.layout.profile_item_picture, parent, false)

        return ProfilePictureViewHolder(view)
    }
    // 데이터 설정
    override fun onBindViewHolder(holder: ProfilePictureViewHolder, position: Int) {
        Glide.with(context)
            .load(imageList[position])
            .into(holder.uploadPicture)
    }
    // 아이템 개수
    override fun getItemCount(): Int {
        return imageList.size
    }

    class ProfilePictureViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val uploadPicture: ImageView = view.findViewById(R.id.profile_picture)
    }
}