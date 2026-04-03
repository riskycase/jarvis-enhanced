package com.riskycase.jarvisEnhanced.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.riskycase.jarvisEnhanced.models.MusicListenEntry

@Dao
interface MusicListenDao {
    @Insert
    fun add(entry: MusicListenEntry)

    @Query("SELECT * FROM musiclistenentry ORDER BY startTime DESC")
    fun getAllLive(): LiveData<List<MusicListenEntry>>

    @Query("SELECT * FROM musiclistenentry WHERE startTime >= :since ORDER BY startTime DESC")
    fun getSinceLive(since: Long): LiveData<List<MusicListenEntry>>

    @Query("SELECT * FROM musiclistenentry WHERE startTime >= :since ORDER BY startTime DESC")
    fun getSince(since: Long): List<MusicListenEntry>

    @Query("DELETE FROM musiclistenentry")
    fun clear()
}
