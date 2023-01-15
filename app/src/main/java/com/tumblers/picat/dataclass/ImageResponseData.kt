package com.tumblers.picat.dataclass


import com.google.gson.JsonArray

data class ImageResponseData(
    val friends: JsonArray?,
    val url: List<String>,
    val img_cnt: Int
)
