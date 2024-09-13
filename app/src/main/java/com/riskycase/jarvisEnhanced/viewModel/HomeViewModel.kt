package com.riskycase.jarvisEnhanced.viewModel

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.repository.SnapRepository
import com.riskycase.jarvisEnhanced.util.NotificationMaker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val snapRepository: SnapRepository,
    private val notificationMaker: NotificationMaker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun getAllSnaps(): LiveData<List<Snap>> {
        return snapRepository.allSnapsLive
    }

    fun clear() {
        Thread {
            snapRepository.clear()
            notificationMaker.clear()
        }.start()
    }

    fun refreshSnaps() {
    }

    fun getRelativeTimestamp(snap: Snap): String {
        return DateUtils.getRelativeDateTimeString(
            context, snap.sent, DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0
        ).toString()
    }

}