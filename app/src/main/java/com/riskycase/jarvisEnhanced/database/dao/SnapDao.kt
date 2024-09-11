package com.riskycase.jarvisEnhanced.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.riskycase.jarvisEnhanced.models.Snap

@Dao
interface SnapDao {
    @Insert
    fun add(snap: Snap)

    @Query("SELECT * FROM snap ORDER BY sent DESC")
    fun getAll(): List<Snap>

    @Query("SELECT * FROM snap ORDER BY sent DESC")
    fun getAllLive(): LiveData<List<Snap>>

    @Query("DELETE FROM snap")
    fun clear()
}