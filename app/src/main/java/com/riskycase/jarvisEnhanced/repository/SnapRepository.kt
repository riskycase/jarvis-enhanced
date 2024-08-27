package com.riskycase.jarvisEnhanced.repository

import android.app.Application
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.SnapDao

class SnapRepository(application: Application) {

    private val snapDao: SnapDao = AppDatabase.getDatabase(application).snapDao()

    val allSnaps = snapDao.getAllLive()

    fun clear() {
        snapDao.clear()
    }
}