package com.tumblers.picat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import com.tumblers.picat.adapter.PictureAdapter

class PictureSelectActivity : AppCompatActivity() {

    lateinit var pictureAdapter: PictureAdapter

    //드래그 선택 리사이클러뷰 관련
    private var mMode = DragSelectionProcessor.Mode.Simple
    private lateinit var mDragSelectTouchListener: DragSelectTouchListener
    private lateinit var mDragSelectionProcessor: DragSelectionProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_select)

//        pictureAdapter = PictureAdapter(imageList, this, startSelecting, selectionList, selectionIdList)
//        mDragSelectionProcessor = DragSelectionProcessor(object :
//            DragSelectionProcessor.ISelectionHandler {
//            override fun getSelection(): HashSet<Int>? {
//                return pictureAdapter.getSelection()
//            }
//
//            override fun isSelected(index: Int): Boolean {
//                return pictureAdapter.getSelection().contains(index)
//            }
//
//            override fun updateSelection(
//                start: Int,
//                end: Int,
//                isSelected: Boolean,
//                calledFromOnStart: Boolean
//            ) {
//                pictureAdapter.selectRange(start, end, isSelected)
//            }
//        }).withMode(mMode)
//        mDragSelectTouchListener = DragSelectTouchListener().withSelectListener(mDragSelectionProcessor)
//        binding.pictureRecyclerview.addOnItemTouchListener(mDragSelectTouchListener)
//        pictureAdapter.setClickListener(object :PictureAdapter.ItemClickListener {
//            override fun onItemClick(view: View?, position: Int) {
//                if (startSelecting){
//                    pictureAdapter.toggleSelection(view, position)
//                }else{
//                    //사진 확대
//                }
//
//            }
//
//            override fun onItemLongClick(view: View?, position: Int): Boolean {
//                if (!startSelecting) {
//                    startSelecting = true
//                    mDragSelectTouchListener.startDragSelection(position)
//                    setRecyclerView()
//                }
////                setRecyclerView()
//                println("롱클릭 선택 시작")
//
//                return true
//            }
//
//        })
    }
}