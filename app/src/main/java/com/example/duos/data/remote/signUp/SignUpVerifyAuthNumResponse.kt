package com.example.duos.data.remote.signUp

import com.google.gson.annotations.SerializedName

data class SignUpVerifyAuthNumResultData (@SerializedName("isCertified") val isPhoneNumAvail: Boolean,
                             @SerializedName("resultMessage") val resultMessage: String)

data class SignUpVerifyAuthNumResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: SignUpVerifyAuthNumResultData
)
