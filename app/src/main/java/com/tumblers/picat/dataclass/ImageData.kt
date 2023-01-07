package com.tumblers.picat.dataclass

import com.google.gson.annotations.SerializedName

data class ImageData(
    val url: List<String>,
    val img_cnt: Int
)

class Data(
    @SerializedName("url") var url: String,
)
