package com.henrisusanto.creativeislandhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.henrisusanto.creativeislandhub.ui.screens.CategoriesScreen
import com.henrisusanto.creativeislandhub.ui.screens.HomeScreen
import com.henrisusanto.creativeislandhub.ui.screens.LikedScreen
import com.henrisusanto.creativeislandhub.ui.screens.SettingsScreen
import com.henrisusanto.creativeislandhub.ui.theme.CreativeIslandHubTheme
import com.henrisusanto.creativeislandhub.ui.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Setup UMP & Ads
        viewModel.adManager.requestConsentAndInit(this) {
            // Ads initialized after consent
        }

        setContent {
            CreativeIslandHubTheme {
                MainScreen(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelResId: Int) {
    object Home : Screen("home", Icons.Default.Home, R.string.title_home)
    object Liked : Screen("liked", Icons.Default.Favorite, R.string.title_liked)
    object Categories : Screen("categories", Icons.Default.List, R.string.title_categories)
    object Settings : Screen("settings", Icons.Default.Settings, R.string.title_settings)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Liked, Screen.Categories, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Liked.route) { LikedScreen(viewModel) }
            composable(Screen.Categories.route) { CategoriesScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
