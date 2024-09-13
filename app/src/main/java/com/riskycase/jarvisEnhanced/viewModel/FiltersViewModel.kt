package com.riskycase.jarvisEnhanced.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(private val filterRepository: FilterRepository) :
    ViewModel() {

    fun getAllFilters(): LiveData<List<Filter>> {
        return filterRepository.allFilters
    }

    fun reset() {
        Thread {
            filterRepository.reset()
        }.start()
    }

    fun deleteFilter(id: Int) {
        Thread {
            filterRepository.delete(id)
        }.start()
    }

}