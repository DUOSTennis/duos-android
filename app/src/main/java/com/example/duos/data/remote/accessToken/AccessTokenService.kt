package com.example.duos.data.remote.accessToken

import android.content.Intent
import android.util.Log
import com.example.duos.AccessTokenView
import com.example.duos.ApplicationClass
import com.example.duos.data.local.UserDatabase
import com.example.duos.data.remote.logout.LogOutService
import com.example.duos.data.remote.logout.LogoutListView
import com.example.duos.ui.splash.SplashActivity
import com.example.duos.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object AccessTokenService : LogoutListView {
    fun getAccessToken(accessTokenView: AccessTokenView) {
        val retrofit = ApplicationClass.retrofit
        var code : Int? = null
        val getAccessTokenService = retrofit.create(AccessTokenRetrofitInterface::class.java)

        getAccessTokenService.getAccessToken(com.example.duos.utils.getAccessToken()!!, getRefreshToken()!!, getUserIdx()!!).enqueue(object :
            Callback<AccessTokenResponse> {
            override fun onResponse(
                call: Call<AccessTokenResponse>,
                response: Response<AccessTokenResponse>
            ) {

                val resp = response.body()!!
                Log.d("resp", resp.toString())
                code = resp.code

                when (resp.code) {
                    1000 -> {
                        saveJwt(resp.result.jwtAccessToken)
                        saveRefreshJwt(resp.result.jwtRefreshToken)
                        accessTokenView.onAccessTokenCode(true)
                    }
                    else -> {
                        logout()
                        accessTokenView.onAccessTokenCode(false)

                    }
                }
            }

            override fun onFailure(call: Call<AccessTokenResponse>, t: Throwable) {
                Log.d("${ApplicationClass.TAG}/API-ERROR", t.message.toString())
            }
        })
    }

    fun logout(){
        LogOutService.postLogOut(this, getUserIdx()!!)
    }

    override fun onLogOutSuccess() {
        Log.d("로그아웃","성공")

        val db = UserDatabase.getInstance(ApplicationClass.getInstance().context()) // 룸에 내 idx에 맞는 데이터 있으면 불러오기.
        if (db != null) {
            db.userDao().clearAll()
        }

        // sharedPreference의 데이터 삭제
        deleteSharedPreferenceData()
        var testWithdrawal = ""
        if(getUserIdx() == 0){
            testWithdrawal =  "성공함"
        }else {
            testWithdrawal = "실패함"
        }

        // 초기화면 SplashActivity로 가기
        val intent = Intent(ApplicationClass.getInstance().context(), SplashActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        ApplicationClass.getInstance().context().startActivity(intent)
    }

    override fun onLogOutFailure(code: Int, message: String) {
        Log.d("로그아웃","실패")
    }
}