package com.tumblers.picat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.ActionBar


private const val ARG_PARAM1 = "param1"


class DownloadCompleteFragment : Fragment() {
    private var param1: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val activity = inflater.context as SharePictureActivity
        val actionbar: ActionBar? = activity.supportActionBar
        actionbar?.hide()
        activity.binding.openBottomsheetFab.visibility = View.INVISIBLE

        // 여기서부터 다시!
        // 프래그먼트에 있는 x버튼을 눌렀을 때의 인터랙션을 하고싶었음
        var fragView = inflater.inflate(R.layout.fragment_download_complete, container, false)
        fragView.findViewById<ImageButton>(R.id.exit_button).setOnClickListener {
            val intent = Intent(getActivity(), MainActivity::class.java)
            startActivity(intent)
            activity.finish()
        }
        return fragView
    }


    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DownloadCompleteFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}