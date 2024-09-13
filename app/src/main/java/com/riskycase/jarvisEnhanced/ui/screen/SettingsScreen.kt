package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel, navController: NavController, drawerState: DrawerState
) {
    Scaffold(
        topBar = { TopBarComponent(navController, drawerState, "Settings", false) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(1f)
        ) {
            Column(modifier = Modifier
                .clickable {
                    settingsViewModel.openNotificationListenerSettings()
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Notification Access", fontSize = 24.sp)
                if (settingsViewModel.getNotificationListenerServiceEnabled()) Text(
                    "Granted", color = Color.Green
                ) else Text("Not granted", color = Color.Red)
            }
            Divider()
            Column(modifier = Modifier
                .clickable {
                    settingsViewModel.openUsageAccessSettings()
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Usage Access", fontSize = 24.sp)
                if (settingsViewModel.getUsageAccessEnabled()) Text(
                    "Granted", color = Color.Green
                ) else Text("Not granted", color = Color.Red)
            }
            Divider()
            Column(modifier = Modifier
                .clickable {
                    settingsViewModel.refreshSnaps()
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Refresh snaps", fontSize = 20.sp)
            }
            Divider()
            Column(modifier = Modifier
                .clickable {
                    settingsViewModel.restartService()
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Restart listener service", fontSize = 20.sp)
            }
        }
    }
}