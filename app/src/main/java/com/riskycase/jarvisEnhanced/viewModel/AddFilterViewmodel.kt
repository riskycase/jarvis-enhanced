package com.riskycase.jarvisEnhanced.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.repository.FilterRepository
import com.riskycase.jarvisEnhanced.util.Constants

class AddFilterViewModel(application: Application) : AndroidViewModel(application) {

    private val filterRepository = FilterRepository(application)

    var currentFilter: MutableLiveData<Filter> =
        MutableLiveData(Filter(0, "", "", Constants.SNAPCHAT_PACKAGE_NAME))

    fun setId(id: Int) {
        if (id == 0)
            currentFilter.value = Filter(0, "", "", Constants.SNAPCHAT_PACKAGE_NAME)
        else
            Thread {
                currentFilter.value = filterRepository.getFilter(id)
            }.start()
    }

    fun save() {
        Thread {
            currentFilter.value?.let { filterRepository.add(it) }
        }.start()
    }

}