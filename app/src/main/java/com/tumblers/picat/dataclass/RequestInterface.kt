package com.tumblers.picat.dataclass

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface RequestInterface {
    @Multipart
    @POST("image/")
    fun postImg(
        @Part arrayImage: MutableList<MultipartBody.Part>,
        @Part ("img_cnt") img_cnt: Int,
        //@Part ("img_list") img_list: ArrayList<Data>

    ): Call<ImageData>
}
