package com.riskycase.jarvisEnhanced.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import com.riskycase.jarvisEnhanced.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFilterViewModel @Inject constructor(private val filterRepository: FilterRepository) :
    ViewModel() {

    var currentFilterTitle = MutableStateFlow("")

    var currentFilterText = MutableStateFlow("")

    private var currentFilterId: Int = 0
    private var currentFilterPackage: String = Constants.SNAPCHAT_PACKAGE_NAME

    fun setId(id: Int) {
        currentFilterId = id
        if (id == 0) {
            currentFilterTitle.update { "" }
            currentFilterText.update { "" }
        } else viewModelScope.launch(Dispatchers.IO) {
            val filter = filterRepository.getFilter(id)
            currentFilterTitle.update { filter.title }
            currentFilterText.update { filter.text }
        }
    }

    fun setTitle(title: String) {
        currentFilterTitle.update { title }
    }

    fun setText(text: String) {
        currentFilterText.update { text }
    }

    fun save() {
        Thread {
            filterRepository.add(
                Filter(
                    currentFilterId,
                    currentFilterTitle.value,
                    currentFilterText.value,
                    currentFilterPackage
                )
            )
        }.start()
    }

}