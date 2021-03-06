package com.example.duos.config

import android.util.Log
import com.example.duos.ApplicationClass.Companion.X_ACCESS_TOKEN
import com.example.duos.utils.getAccessToken
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class XAccessTokenInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()

        val jwtToken: String? = getAccessToken()

        jwtToken?.let{
            builder.addHeader(X_ACCESS_TOKEN, jwtToken)

        }


        return chain.proceed(builder.build())
    }
}