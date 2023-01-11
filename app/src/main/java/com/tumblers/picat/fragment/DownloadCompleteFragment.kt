package com.tumblers.picat.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.tumblers.picat.SharePictureActivity
import com.tumblers.picat.databinding.FragmentDownloadCompleteBinding.inflate
import java.text.SimpleDateFormat
import java.util.*


class DownloadCompleteFragment : Fragment() {
    private var albumName: String? = null
    private var albumCover: String? = null
    private var pictureCount: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            albumName = it.getString("albumName")
            albumCover = it.getString("albumCover")
            pictureCount = it.getInt("pictureCount")

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        fragmentBinding.downloadAlbumCount.text = "| ${pictureCount}장"

        val longNow = System.currentTimeMillis()
        val tDate = Date(longNow)
        val tDateFormat = SimpleDateFormat("yyyy/MM", Locale("ko", "KR"))
        fragmentBinding.downloadAlbumDate.text = tDateFormat.format(tDate)

        Glide.with(activity)
            .load(albumCover?.toUri())
            .into(fragmentBinding.albumCoverImageview)

        fragmentBinding.exitButton.setOnClickListener {
            val intent = Intent(getActivity(), SharePictureActivity::class.java)
            startActivity(intent)
            activity.finish()
        }

        return fragmentBinding.root
    }

}