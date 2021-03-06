package com.example.duos.ui.main.dailyMatching.dailyMatchingSearch

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDialog
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duos.R
import com.example.duos.data.entities.dailyMatching.SearchHistory
import com.example.duos.data.entities.dailyMatching.SearchHistoryDatabase
import com.example.duos.data.remote.dailyMatching.DailyMatchingListService
import com.example.duos.data.remote.dailyMatching.DailyMatchingSearchResultData
import com.example.duos.data.remote.dailyMatching.SearchResultItem
import com.example.duos.databinding.ActivityDailyMatchingSearchBinding
import com.example.duos.ui.BaseActivity
import com.example.duos.ui.adapter.DailyMatchingSearchRVAdapter
import com.example.duos.ui.main.appointment.DailyMatchingSearchHistoryRVAdapter
import com.example.duos.ui.main.dailyMatching.DailyMatchingDetail
import com.example.duos.ui.main.dailyMatching.DailyMatchingSearchView
import com.example.duos.utils.getUserIdx


class DailyMatchingSearchActivity :
    BaseActivity<ActivityDailyMatchingSearchBinding>(ActivityDailyMatchingSearchBinding::inflate),
    DailyMatchingSearchView {
    val userIdx = getUserIdx()
    val TAG = "DailyMatchingSearchActivity"
    private lateinit var searchHistoryAdapter: DailyMatchingSearchHistoryRVAdapter
    private var layoutManager: RecyclerView.LayoutManager? = null
    lateinit var dailyMatchingSearchSearchRVAdapter: DailyMatchingSearchRVAdapter
    private var dailyMatchingSearchListDatas = ArrayList<SearchResultItem>()
    private lateinit var progressDialog: AppCompatDialog


    @SuppressLint("NotifyDataSetChanged")
    override fun initAfterBinding() {

        initView()
//         ?????? ??????
//        val allDailyMatchingSearchRV = binding.allDailyMatchingRecyclerviewRc
//        allDailyMatchingSearchRV.setHasFixedSize(true)
//        allDailyMatchingSearchRV.itemAnimator = DefaultItemAnimator()
//        layoutManager = LinearLayoutManager(this)
//        allDailyMatchingSearchRV.layoutManager = layoutManager
//        dailyMatchingSearchSearchRVAdapter =
//            DailyMatchingSearchRVAdapter(dailyMatchingSearchListDatas)
//        allDailyMatchingSearchRV.adapter = dailyMatchingSearchSearchRVAdapter
//        allDailyMatchingSearchRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.dailyMatchingSearchEt.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == MotionEvent.ACTION_DOWN) {
                // TODO : ?????? ??? ?????? ?????? ?????????????????? binding.dailyMatchingSearchRecordRv ??? ????????????
                search(binding.dailyMatchingSearchEt.text.toString())
//                searchProgressON()
//                progressON()
                return@setOnKeyListener true
            } else {
                return@setOnKeyListener false
            }
        }
        // ?????? ?????? ?????? ??????
        binding.dailyMatchingDeleteSearchRecordTv.setOnClickListener {
            val db = SearchHistoryDatabase.getInstance(this)
            db!!.searchHistoryRoomDao().clearAll()
            searchHistoryAdapter.notifyDataSetChanged()
            binding.dailyMatchingSearchRecordRv.visibility = View.GONE
            binding.dailyMatchingSearchRecordNullTv.visibility = View.VISIBLE
            binding.dailyMatchingSearchResultCountTv.visibility = View.GONE
        }

        binding.dailyMatchingSearchBackIv.setOnClickListener {
            finish()
        }
    }


    private fun search(keyword: String) {
        // ????????? ????????? ????????? ?????? invisible ??? ?????????
        binding.dailyMatchingSearchResultCountTv.visibility = View.VISIBLE // ???????????? ??????
        binding.dailyMatchingSearchRecordNullTv.visibility = View.GONE  // ?????? ???????????? ????????????.
        // ????????? ????????? RoomDB ??? ??????
        saveSearchKeyword(keyword)
        showResultOfSearch()
        // API ???????????? ?????? ????????????
        DailyMatchingListService.searchDailyMatching(this, userIdx, keyword)
    }

    // ?????? ??? ????????? Room DB??? ??????
    private fun saveSearchKeyword(keyword: String) {
        val db = SearchHistoryDatabase.getInstance(this)
        db!!.searchHistoryRoomDao().insert(SearchHistory(null, keyword))
    }

    // ?????? ????????? ??????
    private fun deleteSearchKeyword(keyword: String) {
        val db = SearchHistoryDatabase.getInstance(this)
        db!!.searchHistoryRoomDao().delete(keyword)
        showResultOfSearch()
    }


    private fun showResultOfSearch() {
        val db = SearchHistoryDatabase.getInstance(this)
        val keywords = db!!.searchHistoryRoomDao().getAll().reversed()

        binding.dailyMatchingSearchRecordRv.visibility = View.VISIBLE
        searchHistoryAdapter.submitList(keywords.orEmpty())
        binding.dailyMatchingSearchResultCountTv.visibility = View.VISIBLE
        binding.dailyMatchingResultOfSearchFl.visibility = View.VISIBLE
    }


    private fun initHistoryRV() {
        searchHistoryAdapter = DailyMatchingSearchHistoryRVAdapter(historyDeleteClickListener = {
            deleteSearchKeyword(it)
        })

        binding.dailyMatchingSearchRecordRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dailyMatchingSearchRecordRv.adapter = searchHistoryAdapter
    }

    private fun initView() {
        binding.dailyMatchingSearchResultCountTv.visibility = View.GONE // ?????? ?????? ??? ()
        binding.dailyMatchingResultOfSearchFl.visibility = View.GONE    // ?????? ?????? ?????? ???
        binding.dailyMatchingSearchRecentCl.visibility = View.VISIBLE

        // ?????? ???????????? ????????? (DB ??? ???????????????) ???????????? ?????? ???????????? ????????????.
        val db = SearchHistoryDatabase.getInstance(this)
        val mySearchHistory = db!!.searchHistoryRoomDao().getAll()
        Log.d(TAG, "????????? DB ??? ???????????? $mySearchHistory , isEmpty? : ${mySearchHistory.isEmpty()}")
        initHistoryRV()
        if (mySearchHistory.isEmpty()) {    // ?????? ??????
            binding.dailyMatchingSearchRecordRv.visibility = View.GONE
            binding.dailyMatchingSearchRecordNullTv.visibility = View.VISIBLE
        } else {    // ????????? ??? ??????
            binding.dailyMatchingSearchRecordRv.visibility = View.VISIBLE
            binding.dailyMatchingSearchRecordNullTv.visibility = View.GONE
            showResultOfSearch()
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onGetSearchViewSuccess(dailyMatchingSearchResultData: DailyMatchingSearchResultData) {
        Log.d(TAG, "API ?????? ??????")
        binding.allDailyMatchingRecyclerviewRc.visibility = View.VISIBLE
        binding.dailyMatchingSearchResultCountTv.visibility = View.VISIBLE // ???????????? ??????
        binding.dailyMatchingSearchRecentCl.visibility = View.GONE

        val allDailyMatchingSearchRV = DailyMatchingSearchRVAdapter(dailyMatchingSearchListDatas)
        val recyclerView = binding.allDailyMatchingRecyclerviewRc
        recyclerView.adapter = allDailyMatchingSearchRV
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        val allDailyMatchingSearchRV = binding.allDailyMatchingRecyclerviewRc
//        allDailyMatchingSearchRV.setHasFixedSize(true)
//        allDailyMatchingSearchRV.itemAnimator = DefaultItemAnimator()
//        layoutManager = LinearLayoutManager(this)
//        allDailyMatchingSearchRV.layoutManager = layoutManager
//        dailyMatchingSearchSearchRVAdapter =
//            DailyMatchingSearchRVAdapter(dailyMatchingSearchListDatas)
//        allDailyMatchingSearchRV.adapter = dailyMatchingSearchSearchRVAdapter
//        allDailyMatchingSearchRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.dailyMatchingSearchResultCountTv.text =
            "?????? ?????? (${dailyMatchingSearchResultData.resultSize})"
        binding.dailyMatchingSearchRecentCl.visibility = View.GONE // ?????? ?????????, ????????????
        binding.dailyMatchingResultOfSearchFl.visibility = View.VISIBLE
        dailyMatchingSearchListDatas.clear()
        dailyMatchingSearchListDatas.addAll(dailyMatchingSearchResultData.searchResult)
        Log.d(TAG, "?????? ?????? ??? addAll ?????? $dailyMatchingSearchListDatas")

        allDailyMatchingSearchRV.clickSearchResultListener(object :
            DailyMatchingSearchRVAdapter.SearchResultItemClickListener {
            override fun onItemClick(searchResultItem: SearchResultItem) {
                //  ?????? ???????????? ??????  // ?????????Idx ????????????
                Log.d(TAG, "????????? ?????? : boardIdx : ${searchResultItem.boardIdx}")
                val intent =
                    Intent(this@DailyMatchingSearchActivity, DailyMatchingDetail::class.java)
                intent.apply { putExtra("boardIdx", searchResultItem.boardIdx) }
                startActivity(intent)
            }
        })
        Handler().postDelayed({
//            progressOFF()
//            searchProgressOFF()
        }, (40 * allDailyMatchingSearchRV.itemCount).toLong())

//        searchProgressOFF()

//        Log.d(TAG, "????????? ?????? : bindViewHolderCount : $bindViewHolderCount")
//        if (bindViewHolderCount <= dailyMatchingSearchSearchRVAdapter.itemCount-5) {
//            dailyMatchingSearchSearchRVAdapter.lastBindListener(object :
//                DailyMatchingSearchRVAdapter.BindLastViewHolderListener {
//                override fun onLastBind() {
//                    Log.d(TAG, "?????? viewBindHolder ?????? -> searchProgressOFF")
//                    searchProgressOFF()
//                    bindViewHolderCount = dailyMatchingSearchSearchRVAdapter.itemCount
//                }
//            })
//        } else {
//            searchProgressOFF()
//
//        }

    }

    override fun onGetSearchViewFailure(code: Int, message: String) {
        progressOFF()
//        searchProgressOFF()
        showToast("???????????? ?????? ?????? ??? ?????? ??????????????????.")
        binding.allDailyMatchingRecyclerviewRc.visibility = View.VISIBLE
        binding.dailyMatchingSearchResultCountTv.visibility = View.GONE // ???????????? ??????
        binding.dailyMatchingSearchRecentCl.visibility = View.VISIBLE

    }

//
//    private fun searchProgressON() {
//        Log.d(TAG, "searchProgressOn : ")
//        progressDialog = AppCompatDialog(this)
//        progressDialog.apply {
//            setCanceledOnTouchOutside(false)
//            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            setContentView(R.layout.progress_loading)
//            show()
//        }
//        val img_loading_framge = progressDialog.findViewById<ImageView>(R.id.iv_frame_loading)
//        val frameAnimation = img_loading_framge?.background as AnimationDrawable
//        img_loading_framge.post(Runnable { frameAnimation.start() })
//    }
//
//    private fun searchProgressOFF() {
//        Log.d(TAG, "searchProgressOFF : ")
//        if (progressDialog != null && progressDialog.isShowing) {
//            progressDialog.dismiss()
//        }
//    }

}

