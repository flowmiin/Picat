package com.tumblers.picat.dataclass

import org.json.JSONArray

data class UserData(
    val id: Long,
    val nickname: String,
    var picture: String,
    val email: String,
    val total_count: Int,
    val elements: JSONArray
)
