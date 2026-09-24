package com.henrisusanto.creativeislandhub

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Setup UMP & Ads
        viewModel.adManager.requestConsentAndInit(this) {
            // Preload rewarded ad if ads enabled
            viewModel.adsConfig.value.rewardedAdUnitId?.let { adUnitId ->
                if (viewModel.adsConfig.value.isAdsEnabled) {
                    viewModel.adManager.loadRewardedAd(adUnitId)
                }
            }
        }

        // Observe and apply dynamic per-app language settings
        lifecycleScope.launch {
            viewModel.language.collect { lang ->
                applyAppLocale(lang)
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            CreativeIslandHubTheme(darkTheme = isDarkTheme) {
                MainScreen(viewModel)
            }
        }
    }

    private fun applyAppLocale(langPref: String) {
        val targetLocaleTag = when (langPref) {
            "EN" -> "en"
            "ID" -> "in"
            else -> {
                // AUTO: Detect OS language
                val sysLocale = Resources.getSystem().configuration.locales[0]
                val languageCode = sysLocale?.language?.lowercase() ?: "en"
                if (languageCode == "in" || languageCode == "id") "in" else "en"
            }
        }

        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.toLanguageTags().contains(targetLocaleTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLocaleTag))
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
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onCategoryClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
