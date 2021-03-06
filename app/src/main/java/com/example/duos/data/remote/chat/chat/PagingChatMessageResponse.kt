package com.example.duos.data.remote.chat.chat

import com.google.gson.annotations.SerializedName

data class PagingChatMessageResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: PagingChatMessageResult
)
