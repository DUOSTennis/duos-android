package com.example.duos.data.entities

import com.google.gson.annotations.SerializedName

data class MakeAppointment(
    @SerializedName("chatroomIdx") val chatroomIdx : String,
    @SerializedName("userIdx") val userIdx : Int,
    @SerializedName("partnerIdx") val partnerIdx : Int,
    @SerializedName("appointmentTime") val appointmentTime : String
)
