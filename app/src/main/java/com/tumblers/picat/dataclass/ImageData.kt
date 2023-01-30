package com.tumblers.picat.dataclass

import java.io.Serializable

data class ImageData(
    val idx: Int,
    val uri: String
): Serializable
