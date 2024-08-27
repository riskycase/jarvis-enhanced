package com.riskycase.jarvisEnhanced.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.AddNewFilterItemComponent
import com.riskycase.jarvisEnhanced.ui.components.FilterListItemComponent
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.FilterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    filterViewModel: FilterViewModel,
    navController: NavController,
    drawerState: DrawerState
) {
    val filters = filterViewModel.getAllFilters().observeAsState(emptyList())
    Scaffold(
        topBar = { TopBarComponent(navController, drawerState,"Filters", true) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = filterViewModel::reset,
                icon = { Icon(Icons.Filled.Refresh, "Reset filters") },
                text = { Text("Reset filters") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(1f)
        ) {
            items(
                items = filters.value,
                itemContent = { filter ->
                    FilterListItemComponent(filter, navController)
                },

            )
            item {
                AddNewFilterItemComponent(
                    navController
                )
            }
        }
    }
}