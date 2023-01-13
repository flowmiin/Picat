package com.tumblers.picat.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tumblers.picat.ImageViewPagerActivity
import com.tumblers.picat.R
import com.tumblers.picat.SelectByPeopleActivity
import com.tumblers.picat.SelectPictureActivity

class ProfilePictureAdapter(var imageList: ArrayList<Uri>,
                            var kakaoIdList: ArrayList<Long?>,
                            var selectionIdList: HashSet<Int>,
                            var context: Context
                            ): RecyclerView.Adapter<ProfilePictureAdapter.ProfilePictureViewHolder>() {

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
            .into(holder.profilePicture)

        holder.profilePicture.setOnClickListener {
            // 클릭 시 selectPictureActivity로 넘어가며 -> 넘어간 이후 api 호출
            var intent = Intent(context, SelectByPeopleActivity::class.java)
            intent.putParcelableArrayListExtra("imageList", imageList)
            intent.putExtra("selectedFriendId", kakaoIdList[position])
            intent.putExtra("selectionIdList", selectionIdList)
            context.startActivity(intent)
        }
    }

    // 아이템 개수
    override fun getItemCount(): Int {
        return imageList.size
    }

    inner class ProfilePictureViewHolder(view: View): RecyclerView.ViewHolder(view){
        val profilePicture: ImageButton = view.findViewById(R.id.profile_picture)

    }
}