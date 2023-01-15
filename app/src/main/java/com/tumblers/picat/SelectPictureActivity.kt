package com.tumblers.picat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor.ISelectionHandler
import com.tumblers.picat.adapter.SelectPictureAdapter
import com.tumblers.picat.databinding.ActivityPictureSelectBinding
import com.tumblers.picat.dataclass.ImageData
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SelectPictureActivity : AppCompatActivity() {

    var imageDataList: ArrayList<ImageData> = ArrayList()
    lateinit var selectionIdList: HashSet<Int>
    private var mMode = DragSelectionProcessor.Mode.ToggleAndUndo
    private lateinit var mDragSelectTouchListener: DragSelectTouchListener
    private lateinit var mAdapter: SelectPictureAdapter
    private lateinit var mDragSelectionProcessor: DragSelectionProcessor
    lateinit var actionbar: androidx.appcompat.widget.Toolbar
    lateinit var binding: ActivityPictureSelectBinding
    var myKakaoId: Long = 0
    lateinit var mSocket: Socket


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (intent.hasExtra("myKakaoId")){
            myKakaoId = intent.getLongExtra("myKakaoId", 0)
        }
        if (intent.hasExtra("selectonIdList")){
            selectionIdList = intent.getSerializableExtra("selectonIdList") as HashSet<Int>
        }
        if (intent.hasExtra("imageDataList")){
            imageDataList = intent.getSerializableExtra("imageDataList") as ArrayList<ImageData>
        }


        actionbar = binding.toolbar
        setSupportActionBar(actionbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back_icn)

        actionbar.title = "사진 선택"

        setRecyclerView()

    }


    // ---------------------
    // Selection Listener
    // ---------------------
    private fun updateSelectionListener() {
        mDragSelectionProcessor.withMode(mMode)
    }

    private fun setRecyclerView(){
        var gridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        binding.testRecyclerview.layoutManager = gridLayoutManager
        mAdapter = SelectPictureAdapter(this, selectionIdList, imageDataList)
        binding.testRecyclerview.adapter = mAdapter

        mAdapter.setClickListener(object : SelectPictureAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                mAdapter.toggleSelection(position)
                actionbar.title = "${mAdapter.getCountSelected()} / ${imageDataList.size} "

            }

            override fun onItemLongClick(view: View?, position: Int): Boolean {
                mDragSelectTouchListener.startDragSelection(position)
                return true
            }

        })

        mDragSelectionProcessor = DragSelectionProcessor(object : ISelectionHandler {
            override fun getSelection(): HashSet<Int> {
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
                actionbar.title = "${mAdapter.getCountSelected()} / ${imageDataList.size} "
            }

        }).withMode(mMode)

        mDragSelectTouchListener = DragSelectTouchListener().withSelectListener(mDragSelectionProcessor)

        updateSelectionListener()
        binding.testRecyclerview.addOnItemTouchListener(mDragSelectTouchListener)
        binding.testRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, 10, includeEdge = true))


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_select_picture, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id){
            //뒤로가기
            android.R.id.home -> {
                finish()
                return true
            }
            //완료버튼
            R.id.complete_button -> {
                // 선택한 사진들 가지고 공유방으로 돌아가기
                val intent = Intent(this, SharePictureActivity::class.java)
                intent.putExtra("selectonIdList", mAdapter.mSelected)
                setResult(1, intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}