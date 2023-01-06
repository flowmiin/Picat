package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.R
import kotlinx.android.extensions.LayoutContainer

class PictureAdapter(private var imageList: ArrayList<Uri>,
                     val context: Context,
                     private val startSelecting: Boolean,
                     val selectionList: MutableMap<String, Uri>) : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {

    var onItemSelectionChangedListener : ((MutableMap<String, Uri>) -> Unit)? = null


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

        val isSelectedButton = holder.containerView.findViewById<ImageButton>(R.id.is_selected_imageview)
        val zoomButton = holder.containerView.findViewById<ImageButton>(R.id.zoom_imagebutton)
        if (startSelecting){
            holder.containerView.isClickable = true
            isSelectedButton.visibility = View.VISIBLE
            zoomButton.visibility = View.VISIBLE

            holder.containerView.setOnClickListener {
                if (selectionList.contains(id)){
                    holder.itemView.isSelected = false
                    isSelectedButton.setImageResource(R.drawable.unselected_icn)
                    selectionList.remove(id)
                }else{
                    holder.itemView.isSelected = true
                    isSelectedButton.setImageResource(R.drawable.selected_icn)
                    selectionList[id] = imageList[position]
                }
                onItemSelectionChangedListener?.let { it(selectionList) }
            }
        }else{
            holder.containerView.isClickable = false
            isSelectedButton.visibility = View.GONE
            zoomButton.visibility = View.GONE

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