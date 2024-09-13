package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.ui.components.SnapListItemComponent
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel, navController: NavController, drawerState: DrawerState
) {
    val snaps = homeViewModel.getAllSnaps().observeAsState(emptyList())
    Scaffold(
        topBar = { TopBarComponent(navController, drawerState, "Snaps", true) },
        floatingActionButton = {
            if (snaps.value.isNotEmpty()) ExtendedFloatingActionButton(onClick = {
                homeViewModel.clear()
            }) {
                Icon(
                    imageVector = Icons.Filled.Clear, contentDescription = "Clear snaps"
                )
                Text("Clear all", modifier = Modifier.padding(4.dp))
            }
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(1f)
        ) {
            items(items = snaps.value, itemContent = { snap: Snap ->
                SnapListItemComponent(
                    sender = snap.sender, time = homeViewModel.getRelativeTimestamp(snap)
                )
            })
        }
    }
}