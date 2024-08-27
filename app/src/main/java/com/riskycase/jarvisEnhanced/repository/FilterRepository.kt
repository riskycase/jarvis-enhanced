package com.riskycase.jarvisEnhanced.repository

import android.app.Application
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.FilterDao
import com.riskycase.jarvisEnhanced.database.dao.SnapDao
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.util.Constants

class FilterRepository(application: Application) {

    private val filterDao: FilterDao = AppDatabase.getDatabase(application).filterDao()

    val allFilters = filterDao.getAllLive()

    fun reset() {
        filterDao.reset()
    }

    fun add(filter: Filter) {
        filterDao.add(filter)
    }

    fun getFilter(id: Int): Filter {
        return filterDao.get(id)
    }
}