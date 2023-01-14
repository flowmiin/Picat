package com.tumblers.picat

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import com.tumblers.picat.adapter.SelectPictureAdapter
import com.tumblers.picat.databinding.ActivityPictureSelectBinding
import com.tumblers.picat.dataclass.ImageResponseData
import com.tumblers.picat.dataclass.RequestInterface
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SelectByPeopleActivity : AppCompatActivity() {

    var indexList: ArrayList<Int> = arrayListOf()

    var imageList: ArrayList<Uri> = arrayListOf()
    var thisImageList: ArrayList<Uri> = arrayListOf()
    lateinit var selectionIdList: HashSet<Int>
    lateinit var thisSelectionIdList: HashSet<Int>


    private var mMode = DragSelectionProcessor.Mode.ToggleAndUndo
    private lateinit var mDragSelectTouchListener: DragSelectTouchListener
    private lateinit var mAdapter: SelectPictureAdapter
    private lateinit var mDragSelectionProcessor: DragSelectionProcessor

    lateinit var actionbar: androidx.appcompat.widget.Toolbar
    lateinit var binding: ActivityPictureSelectBinding

    var selectedFriendId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_by_people)

        if (intent.hasExtra("selectedFriendId")){
            selectedFriendId = intent.getLongExtra("selectedFriendId", 0)
        }
        if (intent.hasExtra("selectonIdList")){
            selectionIdList = intent.getSerializableExtra("selectonIdList") as HashSet<Int>
        }
        if (intent.hasExtra("imageList")){
            imageList = intent.getParcelableArrayListExtra<Uri>("imageList") as ArrayList<Uri>
        }

        // api 호출
        apiRequest(selectedFriendId)

        actionbar = binding.toolbar
        setSupportActionBar(actionbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back_icn)

        actionbar.title = "사진 선택"

        setRecyclerView()
    }

    private fun apiRequest(selectedFriendId: Long) {
        // retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.picat_server))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // APIInterface 객체 생성
        var server: RequestInterface = retrofit.create(RequestInterface::class.java)
        server.postSelectedFriend(selectedFriendId).enqueue(object :
            Callback<ImageResponseData> {

            override fun onResponse(call: Call<ImageResponseData>, response: Response<ImageResponseData>) {
                println("인물별 필터 req ${response.isSuccessful}")
                if (response.isSuccessful){
                    println("인물별 필터 res ${response.body()}")
                    // response 받아서 indexList 만들기
                    for (i in response.body()?.url!!){
                        indexList.add(imageList.indexOf(i.toUri()))
                        thisImageList.add(i.toUri())
                        thisSelectionIdList.add(imageList.indexOf(i.toUri()))
                    }

                    setRecyclerView()
                }
            }

            override fun onFailure(call: Call<ImageResponseData>, t: Throwable) {
                println("이미지 업로드 실패")
            }
        })
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
        mAdapter = SelectPictureAdapter(this, indexList.size, selectionIdList, thisImageList)
        binding.testRecyclerview.adapter = mAdapter

        mAdapter.setClickListener(object : SelectPictureAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                mAdapter.toggleSelection(position)
                actionbar.title = "${mAdapter.getCountSelected()} / ${indexList.size} "

            }

            override fun onItemLongClick(view: View?, position: Int): Boolean {
                mDragSelectTouchListener.startDragSelection(position)
                return true
            }

        })

        mDragSelectionProcessor = DragSelectionProcessor(object :
            DragSelectionProcessor.ISelectionHandler {
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
                actionbar.title = "${mAdapter.getCountSelected()} / ${indexList.size} "
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