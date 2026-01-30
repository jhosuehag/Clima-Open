package com.jhosue.weather.extreme.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jhosue.weather.extreme.presentation.theme.ClimaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        var needReload = false
    }

    private lateinit var viewModel: WeatherViewModel

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized && needReload) {
            // Force reload if requested by Settings
            viewModel.reloadData()
            needReload = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        viewModel = androidx.lifecycle.ViewModelProvider(this)[WeatherViewModel::class.java]
        
        setContent {
            ClimaTheme {
                val navController = rememberNavController()
                // Retrieve the same ViewModel instance
                val state = viewModel.state.value
                val context = LocalContext.current

                // Permission Launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        // Handle permission result if needed
                    }
                )

                // Permission State with Lifecycle Observer
                val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                var hasNotificationPermission by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Request Permission on Start if not granted
                LaunchedEffect(Unit) {
                    if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                         permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    // FORCE IMMEDIATE WORKER EXECUTION (EXPEDITED)
                    // We use Expedited to bypass battery optimization delays
                    // FORCE IMMEDIATE WORKER EXECUTION (EXPEDITED)
                    // We use Expedited to bypass battery optimization delays
                    val request = androidx.work.OneTimeWorkRequest.Builder(com.jhosue.weather.extreme.data.worker.SyncWeatherWorker::class.java)
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "ForceUpdateWork",
                         androidx.work.ExistingWorkPolicy.REPLACE,
                         request
                    )
                }
                
                // MONITOR CLIMÁTICO (FOREGROUND) REMOVED
                // User requested strictly Background Monitoring.
                
                // Note: The OneTimeWorkRequest above (lines 100-107) handles the "Immediate Test"
                // when the app opens, but keeps it "asynchronous" via the Worker.
                
                // MAIN LAYOUT: Box for Z-Ordering
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // 1. BACKGROUND: Navigation Host
                    // This renders the screens (WeatherScreen with list, Settings, etc)
                    NavHost(navController = navController, startDestination = "home") {
                        
                        composable("home") {
                            WeatherScreen(
                                state = state,
                                onLocationClick = { locationName, lat, lng ->
                                    navController.navigate("detail/$locationName/$lat/$lng")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onAddClick = {
                                    navController.navigate("search")
                                },
                                onRetry = viewModel::reloadData,
                                onRefresh = viewModel::refreshAll,
                                onEditToggle = viewModel::toggleEditMode,
                                onDelete = viewModel::deleteLocation,
                                onRename = viewModel::renameLocation,
                                onMoveUp = viewModel::moveLocationUp,
                                onMoveDown = viewModel::moveLocationDown,
                                onDragEnd = viewModel::updateSortOrder,
                                checkCurrentLocation = viewModel::onLocationPermissionGranted
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                state = state,
                                onToggleFahrenheit = viewModel::toggleFahrenheit,
                                onToggleNotifications = viewModel::toggleNotifications,
                                onRestoreDefaults = viewModel::restoreDefaultLocations,
                                navController = navController
                            )
                        }
                        
                        composable("search") {
                            SearchScreen(
                                state = state,
                                onSearch = viewModel::searchLocations,
                                onResultClick = viewModel::addFavorite,
                                navController = navController
                            )
                        }
                        
                        composable(
                            route = "detail/{locationName}/{lat}/{lng}",
                            arguments = listOf(
                                navArgument("locationName") { type = NavType.StringType },
                                navArgument("lat") { type = NavType.FloatType },
                                navArgument("lng") { type = NavType.FloatType }
                            )
                        ) { backStackEntry ->
                            val locationName = backStackEntry.arguments?.getString("locationName")
                            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble()
                            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble()
                            
                            WeatherDetailScreen(
                                locationName = locationName,
                                lat = lat,
                                lng = lng,
                                state = state, 
                                navController = navController,
                                loadDailyForecast = viewModel::loadDailyForecast
                            )
                        }
                    }
                    
                    // 2. FOREGROUND: Clean UI - No Test Button requested
                    // The App now relies on Background Worker for alerts.
                }
            }
        }
    }

    // Helper method removed. Logic is centralized in SyncWeatherWorker.
}