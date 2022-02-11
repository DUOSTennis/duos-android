package com.example.duos.data.entities.lastappointment

import com.google.gson.annotations.SerializedName

data class LastAppointmentResDto(
    @SerializedName("userIdx") var userIdx: Int? = null,
    @SerializedName("appointmentIdx") var appointmentIdx : Int? = null,
    @SerializedName("nickname") var nickname : String = "",
    @SerializedName("profilePhotoUrl") var profilePhotoUrl : String = "",
    @SerializedName("appointmentTime") var appointmentTime : String = "",
    @SerializedName("reviewed") var reviewed : Boolean= false
    )

/*

  ㄴuserIdx	int
  ㄴappointmentIdx	int
  ㄴnickname	String
  ㄴprofilePhotoUrl	String
  ㄴappointmentTime	LocalDateTime
  ㄴisReviewed	boolean
 */