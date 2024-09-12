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
    addFilterViewModel: AddFilterViewModel, navController: NavController, drawerState: DrawerState
) {
    Scaffold(topBar = { TopBarComponent(navController, drawerState, "New filter", false) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                addFilterViewModel.save()
                navController.popBackStack()
            }, icon = { Icon(Icons.Filled.Check, "Save filter") }, text = { Text("Save") })
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = addFilterViewModel.currentFilterTitle,
                    onValueChange = addFilterViewModel::setTitle,
                    label = { Text("Title") })
            }
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = addFilterViewModel.currentFilterText,
                    onValueChange = addFilterViewModel::setText,
                    label = { Text("Text") })
            }
        }
    }
}