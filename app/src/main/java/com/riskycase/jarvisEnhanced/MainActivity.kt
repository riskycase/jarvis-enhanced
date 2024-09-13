package com.riskycase.jarvisEnhanced

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.riskycase.jarvisEnhanced.util.Destinations.EDIT_FILTER
import com.riskycase.jarvisEnhanced.util.Destinations.FILTERS
import com.riskycase.jarvisEnhanced.util.Destinations.HOME
import com.riskycase.jarvisEnhanced.util.Destinations.SETTINGS
import com.riskycase.jarvisEnhanced.util.NotificationMaker
import com.riskycase.jarvisEnhanced.viewModel.AddFilterViewModel
import com.riskycase.jarvisEnhanced.viewModel.FilterViewModel
import com.riskycase.jarvisEnhanced.viewModel.HomeViewModel
import com.riskycase.jarvisEnhanced.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationMaker: NotificationMaker

    @Inject
    lateinit var notificationListener: NotificationListener

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onResume() {

        enableEdgeToEdge()

        super.onResume()

        notificationMaker.setup()
        notificationListener.setup()

        val homeViewModel: HomeViewModel by viewModels()
        val filterViewModel: FilterViewModel by viewModels()
        val addFilterViewModel: AddFilterViewModel by viewModels()
        val settingsViewModel: SettingsViewModel by viewModels()

        setContent {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            JarvisTheme {
                ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
                    ModalDrawerSheet {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                getString(R.string.app_name), fontSize = 24.sp
                            )
                            Divider()
                            NavigationDrawerItem(label = {
                                Text("Snap list")
                            },
                                selected = navController.currentBackStackEntry?.id == HOME,
                                onClick = {
                                    if (navController.currentBackStackEntry?.id != HOME) navController.navigate(
                                        HOME
                                    )
                                    scope.launch {
                                        drawerState.close()
                                    }
                                })
                            NavigationDrawerItem(label = {
                                Text("Filter list")
                            },
                                selected = navController.currentBackStackEntry?.id == FILTERS,
                                onClick = {
                                    if (navController.currentBackStackEntry?.id != FILTERS) navController.navigate(
                                        FILTERS
                                    )
                                    scope.launch {
                                        drawerState.close()
                                    }
                                })
                            Divider()
                            NavigationDrawerItem(label = {
                                Text("Settings")
                            },
                                selected = navController.currentBackStackEntry?.id == SETTINGS,
                                onClick = {
                                    if (navController.currentBackStackEntry?.id != SETTINGS) navController.navigate(
                                        SETTINGS
                                    )
                                    scope.launch {
                                        drawerState.close()
                                    }
                                })
                        }
                    }
                }) {
                    NavHost(
                        navController = navController, startDestination = HOME
                    ) {
                        composable(HOME) {
                            HomeScreen(
                                homeViewModel = homeViewModel,
                                navController = navController,
                                drawerState = drawerState
                            )
                        }
                        composable(FILTERS) {
                            FiltersScreen(
                                filterViewModel = filterViewModel,
                                navController = navController,
                                drawerState = drawerState
                            )
                        }
                        composable(
                            route = "$EDIT_FILTER/{filterId}",
                            arguments = listOf(navArgument("filterId") {
                                type = NavType.IntType
                            })
                        ) {
                            addFilterViewModel.setId(
                                it.arguments!!.getInt(
                                    "filterId"
                                )
                            )
                            AddFilterScreen(
                                addFilterViewModel = addFilterViewModel,
                                navController = navController,
                                drawerState = drawerState
                            )
                        }
                        composable(SETTINGS) {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                navController = navController,
                                drawerState = drawerState
                            )
                        }
                    }
                }
            }
        }
    }
}