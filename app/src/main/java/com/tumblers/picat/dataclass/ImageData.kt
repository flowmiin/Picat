package com.tumblers.picat.dataclass

import okhttp3.MultipartBody

data class ImageData(
    //request members
    val image: MultipartBody.Part,

    //response members
    val location: String
)
