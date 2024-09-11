package com.riskycase.jarvisEnhanced.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.repository.FilterRepository

class FilterViewModel(private val application: Application) : AndroidViewModel(application) {

    private val filterRepository = FilterRepository(application)

    fun getAllFilters(): LiveData<List<Filter>> {
        return filterRepository.allFilters
    }

    fun reset() {
        Thread {
            filterRepository.reset()
        }.start()
    }

}