package com.tumblers.picat.room

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

//@Entity(tableName = "id_table")
//data class MyId(
//    @PrimaryKey val id: Long
//)

@Entity(tableName = "image_table")
data class ImageTable(
    @PrimaryKey val imageUri: String
)

