package com.tumblers.picat.dataclass

import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import retrofit2.http.Part

data class UserData(
    val id: Long,
    val nickname: String,
    var picture: String,
    val email: String,
    val total_count: Int,
    val elements: JSONArray
)
