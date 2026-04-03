package com.riskycase.jarvisEnhanced.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.riskycase.jarvisEnhanced.models.MusicListenEntry
import com.riskycase.jarvisEnhanced.repository.MusicListenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    THIS_YEAR("This year"),
    ALL_TIME("All time")
}

data class MusicStats(
    val totalListeningMs: Long = 0,
    val totalTracks: Int = 0,
    val topSongs: List<Pair<String, Long>> = emptyList(),
    val topArtists: List<Pair<String, Long>> = emptyList(),
    val topAlbums: List<Pair<String, Long>> = emptyList(),
    val topPlayers: List<Pair<String, Long>> = emptyList()
)

@HiltViewModel
class MusicStatsViewModel @Inject constructor(
    private val musicListenRepository: MusicListenRepository
) : ViewModel() {

    private val _selectedPeriod = MutableLiveData(TimePeriod.THIS_WEEK)
    val selectedPeriod: LiveData<TimePeriod> = _selectedPeriod

    private val _stats = MutableLiveData(MusicStats())
    val stats: LiveData<MusicStats> = _stats

    init {
        refreshStats()
    }

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        refreshStats()
    }

    fun refreshStats() {
        Thread {
            val since = getSinceTimestamp(_selectedPeriod.value ?: TimePeriod.THIS_WEEK)
            val entries = musicListenRepository.getSince(since)
            _stats.postValue(computeStats(entries))
        }.start()
    }

    private fun getSinceTimestamp(period: TimePeriod): Long {
        val cal = Calendar.getInstance()
        when (period) {
            TimePeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            TimePeriod.ALL_TIME -> return 0L
        }
        return cal.timeInMillis
    }

    private fun computeStats(entries: List<MusicListenEntry>): MusicStats {
        if (entries.isEmpty()) return MusicStats()
        val topSongs = entries.groupBy { "${it.title} — ${it.artist}" }
            .mapValues { it.value.sumOf { e -> e.durationMs } }
            .entries.sortedByDescending { it.value }
            .take(10).map { it.key to it.value }
        val topArtists = entries.groupBy { it.artist }
            .mapValues { it.value.sumOf { e -> e.durationMs } }
            .entries.sortedByDescending { it.value }
            .take(10).map { it.key to it.value }
        val topAlbums = entries.groupBy { "${it.album} — ${it.artist}" }
            .mapValues { it.value.sumOf { e -> e.durationMs } }
            .entries.sortedByDescending { it.value }
            .take(10).map { it.key to it.value }
        val topPlayers = entries.groupBy { it.playerName }
            .mapValues { it.value.sumOf { e -> e.durationMs } }
            .entries.sortedByDescending { it.value }
            .take(5).map { it.key to it.value }
        return MusicStats(
            totalListeningMs = entries.sumOf { it.durationMs },
            totalTracks = entries.size,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topPlayers = topPlayers
        )
    }
}
