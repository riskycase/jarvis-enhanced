package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.AddFilterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFilterScreen(
    addFilterViewModel: AddFilterViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val filter = addFilterViewModel.currentFilter
    Scaffold(
        topBar = { TopBarComponent(navController, drawerState, "New filter", false) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = addFilterViewModel::save,
                icon = { Icon(Icons.Filled.Check, "Save filter") },
                text = { Text("Save") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Title", modifier = Modifier.padding(0.dp, 8.dp))
                OutlinedTextField(
                    value = filter.value!!.title,
                    onValueChange = { filter.value!!.title = it })
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Text", modifier = Modifier.padding(0.dp, 8.dp))
                OutlinedTextField(
                    value = filter.value!!.text,
                    onValueChange = { filter.value?.text = it })
            }
        }
    }
}