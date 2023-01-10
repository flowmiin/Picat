package com.tumblers.picat.dataclass

import org.json.JSONArray

data class ImageData(
    val friends: JSONArray?,
    val url: List<String>,
    val img_cnt: Int
)
