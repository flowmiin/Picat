package com.tumblers.picat.adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R
import kotlinx.android.extensions.LayoutContainer

class PictureAdapter(private var imageList: ArrayList<Uri>,
                     val context: Context,
                     private val startSelecting: Boolean,
                     val selectionList: MutableList<String>) : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {

    var onItemSelectionChangedListener : ((MutableList<String>) -> Unit)? = null


    // 화면 설정
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.item_picture, parent, false)
        return PictureViewHolder(view)
    }

    // 데이터 설정
    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        val id = getItemId(position).toString()
        holder.containerView.tag = id
        Glide.with(context)
            .load(imageList[position])
            .into(holder.uploadPicture)

        if (selectionList.contains(id)){
            holder.itemView.isSelected = true
        }

        if (startSelecting){
            holder.containerView.isClickable = true

            holder.containerView.setOnClickListener {
                if (selectionList.contains(id)){
                    holder.itemView.isSelected = false
                    selectionList.remove(id)
                }else{
                    holder.itemView.isSelected = true
                    selectionList.add(id as String)
                }
                onItemSelectionChangedListener?.let { it(selectionList) }
            }
        }else{
            holder.containerView.isClickable = false
        }


    }


    // 아이템 개수
    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class PictureViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer {
        val uploadPicture: ImageView = containerView.findViewById(R.id.iv_image)
    }
}