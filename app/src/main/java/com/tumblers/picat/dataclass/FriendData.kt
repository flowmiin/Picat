package com.tumblers.picat.dataclass

import com.google.gson.annotations.SerializedName

data class FriendData(
    val id: Long?,
    var picture : ImageData,
    var nickName : String
)
