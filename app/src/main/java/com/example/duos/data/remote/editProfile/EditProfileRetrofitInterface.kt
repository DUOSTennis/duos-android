package com.example.duos.data.remote.editProfile

import com.example.duos.ApplicationClass.Companion.EDIT_GET_API
import com.example.duos.ApplicationClass.Companion.EDIT_PUT_NON_PIC_API
import com.example.duos.ApplicationClass.Companion.EDIT_PUT_PIC_API
import com.example.duos.data.entities.editProfile.EditProfilePutReqDto
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface EditProfileRetrofitInterface {

    @GET(EDIT_GET_API)
    fun onGetProfile(@Query("userIdx") userIdx: Int): Call<EditProfileResponse>

    @PUT(EDIT_PUT_NON_PIC_API)
    fun onPutProfileWithoutPic(
        @Body editProfilePutReqDto: EditProfilePutReqDto,
        @Query("userIdx") userIdx: Int
    ): Call<EditProfilePutResponse>

    @Multipart
    @PUT(EDIT_PUT_PIC_API)
    fun onPutProfilePic(
        @Part profileImg : MultipartBody.Part?,
        @Query("userIdx") userIdx : Int
    ) : Call<EditProfilePutPicResponse>

}
