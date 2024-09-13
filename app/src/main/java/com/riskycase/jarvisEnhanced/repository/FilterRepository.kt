package com.riskycase.jarvisEnhanced.repository

import android.content.Context
import com.riskycase.jarvisEnhanced.database.AppDatabase
import com.riskycase.jarvisEnhanced.database.dao.FilterDao
import com.riskycase.jarvisEnhanced.models.Filter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FilterRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val filterDao: FilterDao = AppDatabase.getDatabase(context).filterDao()

    val allFilters = filterDao.getAllLive()

    fun reset() {
        filterDao.reset()
    }

    fun add(filter: Filter) {
        if (filter.id == 0) filterDao.add(filter) else filterDao.update(filter)
    }

    fun delete(id: Int) {
        filterDao.delete(id)
    }

    fun getFilter(id: Int): Filter {
        return filterDao.get(id)
    }
}