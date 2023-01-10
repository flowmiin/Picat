package com.tumblers.picat.dataclass

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface RequestInterface {
    @Multipart
    @POST("image/")
    fun postImg(
        @Part arrayImage: MutableList<MultipartBody.Part>,
        @Part ("img_cnt") img_cnt: Int,
        @Part ("id") id: Long
    ): Call<ImageData>

    @POST("app/users/kakao/")
    fun postUser(
        @Body userData: JsonObject
    ) : Call<SimpleResponseData>
}
