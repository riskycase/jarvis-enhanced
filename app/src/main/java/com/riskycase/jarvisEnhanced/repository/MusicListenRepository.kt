package com.riskycase.jarvisEnhanced.repository

import android.content.Context
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.MusicListenDao
import com.riskycase.jarvisEnhanced.models.MusicListenEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MusicListenRepository @Inject constructor(@ApplicationContext context: Context) {

    private val dao: MusicListenDao = AppDatabase.getDatabase(context).musicListenDao()

    fun add(entry: MusicListenEntry) = dao.add(entry)

    fun getSinceLive(since: Long) = dao.getSinceLive(since)

    fun getSince(since: Long) = dao.getSince(since)

    fun clear() = dao.clear()
}
