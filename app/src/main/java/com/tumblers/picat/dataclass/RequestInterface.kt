package com.tumblers.picat.dataclass

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface RequestInterface {
    @Multipart
    @POST("image/")
    fun postImg(
        @Header ("kid") header_id: Long,
        @Part arrayImage: MutableList<MultipartBody.Part>,
        @Part ("img_cnt") img_cnt: Int,
        @Part ("id") id: Long
    ): Call<ImageResponseData>

    @POST("app/users/kakao/")
    fun postUser(
        @Body userData: JsonObject
    ) : Call<SimpleResponseData>

    @FormUrlEncoded
    @POST("friends/")
    fun postFriends(
        @Field ("friends") friendList: MutableSet<Long>,
        @Field ("id")myKakaoId: Long
    ) :Call<SimpleResponseData>

    @GET("filter/")
    fun postSelectedFriend(
        @Query ("id") id: Long
    ) : Call<ImageResponseData>

    @FormUrlEncoded
    @POST("accept/")
    fun postInviteResponse(
        @Field ("id") id: Long?,
        @Field ("roomIdx") roomIdx: Long?
    ) :Call<SimpleResponseData>

    @GET("clear/")
    fun getExceptBlur(
        @Query ("id") id: Long?
    ): Call<ImageResponseData>

    @GET("blur/")
    fun getBlur(
        @Query ("id") id: Long?
    ): Call<ImageResponseData>
}
