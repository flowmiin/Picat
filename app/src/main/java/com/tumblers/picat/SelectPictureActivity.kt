package com.tumblers.picat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor.ISelectionHandler
import com.tumblers.picat.adapter.PictureAdapter
import com.tumblers.picat.adapter.SelectPictureAdapter
import com.tumblers.picat.databinding.ActivityPictureSelectBinding
import com.tumblers.picat.databinding.ActivitySharePictureBinding
import com.tumblers.picat.service.ForegroundService
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class SelectPictureActivity : AppCompatActivity() {

    var imageList: ArrayList<Uri> = ArrayList()
    lateinit var selectionIdList: HashSet<Int>
    private var mMode = DragSelectionProcessor.Mode.ToggleAndUndo
    private lateinit var mDragSelectTouchListener: DragSelectTouchListener
    private lateinit var mAdapter: SelectPictureAdapter
    private lateinit var mDragSelectionProcessor: DragSelectionProcessor
    lateinit var actionbar: androidx.appcompat.widget.Toolbar

    lateinit var binding: ActivityPictureSelectBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPictureSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionbar = binding.toolbar
        setSupportActionBar(actionbar) //커스텀한 toolbar를 액션바로 사용
        supportActionBar?.setDisplayShowTitleEnabled(false) //액션바에 표시되는 제목의 표시유무를 설정합니다. false로 해야 custom한 툴바의 이름이 화면에 보이게 됩니다.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        actionbar.title = "사진 선택"


        selectionIdList = intent.getSerializableExtra("selectonIdList") as HashSet<Int>
        var myKakaoId = intent.getLongExtra("myKakaoId", 0)

        // socket 통신 연결
        var mSocket = SocketApplication.get()
        mSocket.connect()
        mSocket.emit("join", myKakaoId)
        mSocket.on("join", onJoin)

        setRecyclerView()

    }


    // ---------------------
    // Selection Listener
    // ---------------------
    private fun updateSelectionListener() {
        mDragSelectionProcessor.withMode(mMode)
    }

    var onJoin = Emitter.Listener { args->
        CoroutineScope(Dispatchers.Main).launch {
            val img_count = JSONObject(args[0].toString()).getInt("img_cnt")
            val img_list = JSONObject(args[0].toString()).getJSONArray("img_list")
            for (i in 0..img_count - 1) {
                val imgObj = JSONObject(img_list[i].toString()).getString("url")
                imageList.add(imgObj.toString().toUri())
            }
            setRecyclerView()
        }
    }

    private fun setRecyclerView(){
        var gridLayoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        binding.testRecyclerview.layoutManager = gridLayoutManager
        mAdapter = SelectPictureAdapter(this, imageList.size, selectionIdList, imageList)
        binding.testRecyclerview.adapter = mAdapter

        mAdapter.setClickListener(object : SelectPictureAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                mAdapter.toggleSelection(position)
                actionbar.title = "${mAdapter.getCountSelected()} / ${imageList.size} "

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
                actionbar.title = "${mAdapter.getCountSelected()} / ${imageList.size} "
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