package com.tumblers.picat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor.ISelectionHandler
import com.tumblers.picat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var mMode = DragSelectionProcessor.Mode.Simple
    private lateinit var mDragSelectTouchListener: DragSelectTouchListener
    private lateinit var mAdapter: TestAdapter
    private lateinit var mDragSelectionProcessor: DragSelectionProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var gridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        binding.testRecyclerview.layoutManager = gridLayoutManager
        mAdapter = TestAdapter(this, 500, HashSet())
        binding.testRecyclerview.adapter = mAdapter

        mAdapter.setClickListener(object : TestAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                mAdapter.toggleSelection(position)
            }

            override fun onItemLongClick(view: View?, position: Int): Boolean {
                mDragSelectTouchListener.startDragSelection(position)
                return true
            }

        })

        mDragSelectionProcessor = DragSelectionProcessor(object : ISelectionHandler {
            override fun getSelection(): HashSet<Int?> {
                return mAdapter.mSelected
            }

            override fun isSelected(index: Int): Boolean {
                return mAdapter.mSelected.contains(index)
            }

            override fun updateSelection(
                start: Int,
                end: Int,
                isSelected: Boolean,
                calledFromOnStart: Boolean
            ) {
                mAdapter.selectRange(start, end, isSelected)
            }
        })
            .withMode(mMode)
        mDragSelectTouchListener = DragSelectTouchListener()
            .withSelectListener(mDragSelectionProcessor)

        updateSelectionListener()
        binding.testRecyclerview.addOnItemTouchListener(mDragSelectTouchListener)

        binding.fab.setOnClickListener {
            mMode = DragSelectionProcessor.Mode.ToggleAndUndo
            updateSelectionListener()
        }

    }


    // ---------------------
    // Selection Listener
    // ---------------------
    private fun updateSelectionListener() {
        mDragSelectionProcessor.withMode(mMode)
        Toast.makeText(this,"Mode: " + mMode.name, Toast.LENGTH_SHORT).show()
    }
}