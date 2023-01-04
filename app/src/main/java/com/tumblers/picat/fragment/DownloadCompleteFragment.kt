package com.tumblers.picat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.ActionBar
import com.tumblers.picat.MainActivity
import com.tumblers.picat.SharePictureActivity
import com.tumblers.picat.databinding.ActivityMainBinding
import com.tumblers.picat.databinding.FragmentDownloadCompleteBinding
import com.tumblers.picat.databinding.FragmentDownloadCompleteBinding.inflate



class DownloadCompleteFragment : Fragment() {
    private var albumName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            albumName = it.getString("albumName")
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
        fragmentBinding.exitButton.setOnClickListener {
            val intent = Intent(getActivity(), MainActivity::class.java)
            startActivity(intent)
            activity.finish()
        }

        return fragmentBinding.root
    }

}