package com.riskycase.jarvisEnhanced.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.util.Constants

@Dao
interface FilterDao {
    @Insert
    fun add(filter: Filter)

    @Query("SELECT * FROM filter")
    fun getAll(): List<Filter>

    @Query("SELECT * FROM filter")
    fun getAllLive(): LiveData<List<Filter>>

    @Query("DELETE FROM filter")
    fun clear()

    @Update
    fun update(filter: Filter)

    @Query("DELETE FROM filter WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM filter WHERE id = :id")
    fun get(id: Int): Filter

    @Query("SELECT * FROM filter WHERE packageName = :packageName")
    fun get(packageName: String): List<Filter>
    fun reset() {
        clear()
        add(Filter(0, "from $", "", Constants.SNAPCHAT_PACKAGE_NAME))
        add(Filter(0, "", "from $", Constants.SNAPCHAT_PACKAGE_NAME))
        add(Filter(0, "$", "sent a Snap", Constants.SNAPCHAT_PACKAGE_NAME))
        add(Filter(0, "$", "sent you a Snap", Constants.SNAPCHAT_PACKAGE_NAME))
        add(Filter(0, "$", "sent you a Snap.*", Constants.SNAPCHAT_PACKAGE_NAME))
    }
}