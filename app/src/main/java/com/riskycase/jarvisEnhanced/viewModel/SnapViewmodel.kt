package com.riskycase.jarvisEnhanced.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.repository.SnapRepository
import com.riskycase.jarvisEnhanced.util.NotificationMaker

class SnapViewModel(private val application: Application) : AndroidViewModel(application) {

    private val snapRepository = SnapRepository(application)

    fun getAllSnaps(): LiveData<List<Snap>> {
        return snapRepository.allSnapsLive
    }

    fun clear() {
        Thread {
            snapRepository.clear()
            NotificationMaker().clear(application)
        }.start()
    }

    fun refreshSnaps() {

    }

}