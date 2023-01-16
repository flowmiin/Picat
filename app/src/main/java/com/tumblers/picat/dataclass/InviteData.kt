package com.tumblers.picat.dataclass
import java.io.Serializable
data class InviteData(
    var id: Long,
    var roodIdx: Int,
    var picture: String,
    var nickname: String
): Serializable
