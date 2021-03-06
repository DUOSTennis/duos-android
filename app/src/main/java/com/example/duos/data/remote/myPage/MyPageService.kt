package com.example.duos.data.remote.myPage

import android.util.Log
import com.example.duos.ApplicationClass
import com.example.duos.ui.main.mypage.myprofile.MyPageItemView
import com.example.duos.utils.NetworkModule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

object MyPageService {
    private const val TAG: String = "MyPageService"

    //    lateinit var myPageDatas : MyPageItem
    val retrofit = NetworkModule.getRetrofit()
    fun getUserPage(myPageItemView: MyPageItemView, userIdx: Int) {
//        val myPageService = NetworkModule.retrofit.create(MyPageRetrofitInterface::class.java)
        val myPageService = retrofit.create(MyPageRetrofitInterface::class.java)

        myPageService.getUserPage(userIdx).enqueue(object : Callback<MyPageResponse> {
            override fun onResponse(call: Call<MyPageResponse>, response: Response<MyPageResponse>) {
                val resp = response.body()!!
                when (resp.code) {
                    1000 -> {
                        resp.result.let {
                            myPageItemView.onGetMyPageItemSuccess(it)
                            Log.d(TAG, it.userIdx.toString()); Log.d(TAG, it.nickname)
                            Log.d(TAG, it.phoneNumber); Log.d(TAG, it.experience)
                            Log.d(TAG, it.gamesCount.toString())
                        }
                    }
                    else -> {
                        Log.d(TAG, "CODE: ${resp.code}, MESSAGE : ${resp.message}")
                        myPageItemView.onGetMyPageItemFailure(resp.code, resp.message)
                    }
                }
            }

            override fun onFailure(call: Call<MyPageResponse>, t: Throwable) {
                Log.d("${TAG}/API-ERROR", t.message.toString())
//                Log.d(TAG, "CODE: ${resp.code}, MESSAGE : ${resp.message}")
                // 네트워크 오류 발생
                myPageItemView.onGetMyPageItemFailure(400, "네트워크 오류 발생")
            }


        })

    }


}


