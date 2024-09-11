package com.riskycase.jarvisEnhanced.repository

import android.app.Application
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.SnapDao
import com.riskycase.jarvisEnhanced.models.Snap

class SnapRepository(application: Application) {

    private val snapDao: SnapDao = AppDatabase.getDatabase(application).snapDao()

    val allSnaps = snapDao.getAllLive()

    fun add(snap: Snap) {
        return snapDao.add(snap)
    }

    fun clear() {
        snapDao.clear()
    }
}