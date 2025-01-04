package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.SettingsViewModel
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel, navController: NavController, drawerState: DrawerState
) {
    val apiKey by settingsViewModel.currentNasaApiKey.collectAsState()
    val openApiKeyInputDialog = remember { mutableStateOf(false) }

    when {
        openApiKeyInputDialog.value -> {
            Dialog(
                onDismissRequest = { openApiKeyInputDialog.value = false },
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NASA API key", fontSize = 24.sp)
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = settingsViewModel::setCurrentNasaApiKey,
                            label = { Text("API key") })
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                settingsViewModel.resetCurrentNasaApiKey()
                                openApiKeyInputDialog.value = false
                            }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = {
                                settingsViewModel.saveCurrentNasaApiKey()
                                openApiKeyInputDialog.value = false
                            }) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

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
                    settingsViewModel.restartService()
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Refresh snaps and fix service", fontSize = 20.sp)
            }
            Divider()
            Column(modifier = Modifier
                .clickable {
                    openApiKeyInputDialog.value = true
                }
                .padding(16.dp)
                .fillMaxWidth(1f)) {
                Text("Update NASA API key", fontSize = 20.sp)
            }
        }
    }
}