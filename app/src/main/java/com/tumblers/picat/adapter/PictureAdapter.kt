package com.tumblers.picat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.tumblers.picat.R
import kotlinx.android.extensions.LayoutContainer

class PictureAdapter(private var imageList: ArrayList<Uri>,
                     val context: Context,
                     private var startSelecting: Boolean,
                     val selectionList: MutableMap<String, Uri>)
    : RecyclerView.Adapter<PictureAdapter.PictureViewHolder>() {

    //커스텀 클릭 인터페이스를 정의
    interface MyItemClickListener {
        fun onLongItemClicked(position: Int)
        fun onItemClicked(position: Int)
        fun onMultipleViewHoldersSelected(selection: MutableList<ViewHolder?>)
    }

    var onItemSelectionChangedListener : ((MutableMap<String, Uri>) -> Unit)? = null
    private var myItemClickListener : MyItemClickListener? = null

    //액티비티 코드에서 인터페이스 초기화 용도
    fun setMyItemClickListener(itemClickListener: MyItemClickListener){
        myItemClickListener = itemClickListener
    }

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


        val isSelectedButton = holder.containerView.findViewById<ImageButton>(R.id.is_selected_imageview)
        val zoomButton = holder.containerView.findViewById<ImageButton>(R.id.zoom_imagebutton)

        if (selectionList.contains(id)){
            holder.itemView.isSelected = true
            isSelectedButton.setImageResource(R.drawable.selected_icn)
            isSelectedButton.visibility = View.VISIBLE
        }

        //롱 클릭시 사진 선택 시작하기
        holder.containerView.setOnLongClickListener {
            myItemClickListener?.onLongItemClicked(position)
            return@setOnLongClickListener(true)
        }

        // 선택중일때
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
        }
        else{
            // 선택 완료했을 때
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