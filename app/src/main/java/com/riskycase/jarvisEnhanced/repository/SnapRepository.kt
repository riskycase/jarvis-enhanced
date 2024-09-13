package com.riskycase.jarvisEnhanced.repository

import android.content.Context
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.SnapDao
import com.riskycase.jarvisEnhanced.models.Snap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SnapRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val snapDao: SnapDao = AppDatabase.getDatabase(context).snapDao()

    val allSnapsLive = snapDao.getAllLive()

    fun allSnaps(): List<Snap> {
        return snapDao.getAll()
    }

    fun add(snap: Snap) {
        return snapDao.add(snap)
    }

    fun clear() {
        snapDao.clear()
    }
}