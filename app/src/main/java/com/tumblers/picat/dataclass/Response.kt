package com.tumblers.picat.dataclass

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ResponseDC(var result:String? = null)

interface APIInterface {
    @Multipart
    @POST("/image")
    fun postImg(@Part photo: MultipartBody.Part): Call<ResponseDC>
}
