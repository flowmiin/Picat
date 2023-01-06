package com.tumblers.picat.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.tumblers.picat.FriendListActivity
import com.tumblers.picat.SharePictureActivity
import com.tumblers.picat.databinding.FragmentDownloadCompleteBinding.inflate



class DownloadCompleteFragment : Fragment() {
    private var albumName: String? = null
    private var firstPictureUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            albumName = it.getString("albumName")
            firstPictureUri = it.getString("firstPicture")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val activity = inflater.context as SharePictureActivity
        val actionbar: ActionBar? = activity.supportActionBar
        actionbar?.hide()
        activity.binding.openBottomsheetFab.visibility = View.INVISIBLE


        var fragmentBinding = inflate(inflater, container, false)
        fragmentBinding.downloadAlbumTitle.text = albumName
//        Glide.with(context)
//            .load(imageList[position])
//            .into(holder.uploadPicture)
//        fragmentBinding.albumCoverImageview.
        fragmentBinding.exitButton.setOnClickListener {
            val intent = Intent(getActivity(), SharePictureActivity::class.java)
            startActivity(intent)
            activity.finish()
        }

        return fragmentBinding.root
    }

}