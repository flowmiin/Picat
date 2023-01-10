package com.tumblers.picat.room

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

//@Dao
//interface IdDao {
//    @Query("SELECT * FROM id_table")
//    fun get(): Long?
//
//    @Insert
//    fun insert(id: MyId)
//
//    @Delete
//    fun delete(id: MyId)
//}

@Dao
interface ImageDao {
    @Query("SELECT * FROM image_table")
    fun getAll(): List<ImageTable>

    @Insert
    fun insert(image: ImageTable)

    @Delete
    fun delete(image: ImageTable)
}