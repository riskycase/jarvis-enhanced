package com.riskycase.jarvisEnhanced

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.riskycase.jarvisEnhanced.service.NotificationListener
import com.riskycase.jarvisEnhanced.ui.screen.AddFilterScreen
import com.riskycase.jarvisEnhanced.ui.screen.FiltersScreen
import com.riskycase.jarvisEnhanced.ui.screen.HomeScreen
import com.riskycase.jarvisEnhanced.ui.screen.SettingsScreen
import com.riskycase.jarvisEnhanced.ui.theme.JarvisTheme
import com.riskycase.jarvisEnhanced.util.Converter
import com.riskycase.jarvisEnhanced.util.Destinations.EDIT_FILTER
import com.riskycase.jarvisEnhanced.util.Destinations.FILTERS
import com.riskycase.jarvisEnhanced.util.Destinations.HOME
import com.riskycase.jarvisEnhanced.util.Destinations.SETTINGS
import com.riskycase.jarvisEnhanced.util.NotificationMaker
import com.riskycase.jarvisEnhanced.viewModel.AddFilterViewModel
import com.riskycase.jarvisEnhanced.viewModel.FilterViewModel
import com.riskycase.jarvisEnhanced.viewModel.SettingsViewModel
import com.riskycase.jarvisEnhanced.viewModel.SnapViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onResume() {
        super.onResume()

        NotificationMaker().setup(applicationContext)
        NotificationListener().setup(applicationContext)
        val snapViewModel = ViewModelProvider(this)[SnapViewModel::class.java]
        val filterViewModel = ViewModelProvider(this)[FilterViewModel::class.java]
        val addFilterViewModel = ViewModelProvider(this)[AddFilterViewModel::class.java]
        val settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            JarvisTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp),
                                verticalArrangement = Arrangement
                                    .spacedBy(8.dp)
                            ) {
                                Text(
                                    getString(R.string.app_name),
                                    fontSize = 24.sp
                                )
                                Divider()
                                NavigationDrawerItem(
                                    label = {
                                        Text("Snap list")
                                    },
                                    selected = navController.currentBackStackEntry?.id == HOME,
                                    onClick = {
                                        if (navController.currentBackStackEntry?.id != HOME)
                                            navController.navigate(HOME)
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = {
                                        Text("Filter list")
                                    },
                                    selected = navController.currentBackStackEntry?.id == FILTERS,
                                    onClick = {
                                        if (navController.currentBackStackEntry?.id != FILTERS)
                                            navController.navigate(FILTERS)
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
                {
                    NavHost(
                        navController = navController,
                        startDestination = HOME
                    ) {
                        composable(HOME) {
                            HomeScreen(
                                snapViewModel,
                                navController,
                                drawerState,
                                Converter(applicationContext)
                            )
                        }
                        composable(FILTERS) {
                            FiltersScreen(
                                filterViewModel,
                                navController,
                                drawerState
                            )
                        }
                        composable(
                            route = "$EDIT_FILTER/{filterId}",
                            arguments = listOf(
                                navArgument("filterId") { type = NavType.IntType }
                            )
                        ) {
                            addFilterViewModel.setId(it.arguments!!.getInt("filterId"))
                            AddFilterScreen(
                                addFilterViewModel,
                                navController,
                                drawerState
                            )
                        }
                        composable(SETTINGS) {
                            SettingsScreen(
                                settingsViewModel,
                                navController,
                                drawerState
                            )
                        }
                    }
                }
            }
        }
    }
}