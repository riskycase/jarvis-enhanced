package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.MusicBlocklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicBlocklistScreen(
    musicBlocklistViewModel: MusicBlocklistViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val apps = musicBlocklistViewModel.apps.observeAsState(emptyList())

    Scaffold(
        topBar = { TopBarComponent(navController, drawerState, "Blocked Apps", false) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
        ) {
            items(items = apps.value, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { musicBlocklistViewModel.toggleApp(app.packageName) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = app.blocked,
                        onCheckedChange = { musicBlocklistViewModel.toggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}
