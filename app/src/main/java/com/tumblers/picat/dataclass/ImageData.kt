package com.tumblers.picat.dataclass

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody

data class ImageData(
    //request members
    val arrayImage: MutableList<MultipartBody.Part>,
    val img_cnt: Int,

    //response members
    val img_list: ArrayList<Data>
)

class Data(
    @SerializedName("url") var url: String,
    @SerializedName("img_cnt") var img_cnt: Int
)
