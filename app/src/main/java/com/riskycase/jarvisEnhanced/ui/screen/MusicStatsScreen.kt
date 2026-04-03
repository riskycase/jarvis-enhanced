package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.MusicStats
import com.riskycase.jarvisEnhanced.viewModel.MusicStatsViewModel
import com.riskycase.jarvisEnhanced.viewModel.TimePeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStatsScreen(
    musicStatsViewModel: MusicStatsViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val selectedPeriod = musicStatsViewModel.selectedPeriod.observeAsState(TimePeriod.THIS_WEEK)
    val stats = musicStatsViewModel.stats.observeAsState(MusicStats())

    Scaffold(
        topBar = { TopBarComponent(navController, drawerState, "Music Stats", true) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod.value == period,
                            onClick = { musicStatsViewModel.selectPeriod(period) },
                            label = { Text(period.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Tracks", stats.value.totalTracks.toString())
                    StatCard("Time", formatDuration(stats.value.totalListeningMs))
                }
            }

            if (stats.value.topSongs.isNotEmpty()) {
                item { SectionHeader("Top Songs") }
                itemsIndexed(stats.value.topSongs) { index, (name, duration) ->
                    RankedItem(index + 1, name, formatDuration(duration))
                }
            }

            if (stats.value.topArtists.isNotEmpty()) {
                item { SectionHeader("Top Artists") }
                itemsIndexed(stats.value.topArtists) { index, (name, duration) ->
                    RankedItem(index + 1, name, formatDuration(duration))
                }
            }

            if (stats.value.topAlbums.isNotEmpty()) {
                item { SectionHeader("Top Albums") }
                itemsIndexed(stats.value.topAlbums) { index, (name, duration) ->
                    RankedItem(index + 1, name, formatDuration(duration))
                }
            }

            if (stats.value.topPlayers.isNotEmpty()) {
                item { SectionHeader("Players") }
                itemsIndexed(stats.value.topPlayers) { index, (name, duration) ->
                    RankedItem(index + 1, name, formatDuration(duration))
                }
            }

            item { /* bottom spacing */ }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun RankedItem(rank: Int, name: String, duration: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$rank. $name",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        Text(duration, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
