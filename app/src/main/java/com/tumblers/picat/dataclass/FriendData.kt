package com.tumblers.picat.dataclass

import com.google.gson.annotations.SerializedName

data class FriendData(
    val id: Long?,
    val uuid: String,
    val profile_nickname: String?,
    var profile_thumbnail_image: String?,
    val favorite: Boolean?,
    val allowedMsg: Boolean?,
//    val serviceUserId:
)
