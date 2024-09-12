package com.riskycase.jarvisEnhanced.viewModel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import com.riskycase.jarvisEnhanced.util.Constants

class AddFilterViewModel(application: Application) : AndroidViewModel(application) {

    private val filterRepository = FilterRepository(application)

    var currentFilterTitle: String by mutableStateOf("")
        private set

    var currentFilterText: String by mutableStateOf("")
        private set

    private var currentFilterId: Int = 0
    private var currentFilterPackage: String = Constants.SNAPCHAT_PACKAGE_NAME

    fun setId(id: Int) {
        currentFilterId = id
        if (id == 0) {
            currentFilterTitle = ""
            currentFilterText = ""
        } else Thread {
            val filter = filterRepository.getFilter(id)
            currentFilterTitle = filter.title
            currentFilterText = filter.text
        }.start()
    }

    fun setTitle(title: String) {
        currentFilterTitle = title
    }

    fun setText(text: String) {
        currentFilterText = text
    }

    fun save() {
        Thread {
            filterRepository.add(
                Filter(
                    currentFilterId, currentFilterTitle, currentFilterText, currentFilterPackage
                )
            )
        }.start()
    }

}