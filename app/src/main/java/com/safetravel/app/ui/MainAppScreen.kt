package com.safetravel.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.safetravel.app.service.BackgroundSafetyService
import com.safetravel.app.ui.common.SosButton
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.common.SosState
import com.safetravel.app.ui.home.HomeScreen
import com.safetravel.app.ui.profile.ContactsScreen
import com.safetravel.app.ui.profile.ProfileScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Map : BottomNavItem("map", "Map", Icons.Default.LocationOn)
    object Pedestrian : BottomNavItem("pedestrian", "Sensor", Icons.Default.CompassCalibration)
    object Contacts : BottomNavItem("contacts", "Contacts", Icons.Default.People)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    rootNavController: NavHostController,
    viewModel: MainAppViewModel = hiltViewModel(),
    sosViewModel: SosButtonViewModel = hiltViewModel() // Create the single instance here
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    
    val mainAppUiState by viewModel.uiState.collectAsState()
    val sosUiState by sosViewModel.uiState.collectAsState()

    // --- Centralized SOS Navigation Logic ---
    LaunchedEffect(sosUiState.sosState) {
        if (sosUiState.sosState is SosState.NavigateToAiHelp) {
            if (currentRoute != BottomNavItem.Map.route) {
                // If not on the map, go there first. 
                // The InTripScreen will then handle the next navigation step.
                bottomNavController.navigate(BottomNavItem.Map.route) { 
                    popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                // If already on the map, navigate directly to AI help
                rootNavController.navigate("ai_help")
                sosViewModel.onNavigatedToAiHelp() // Reset state after navigation
            }
        }
    }

    // --- Permissions & Service Logic ---
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    val isLocationGranted = permissionState.permissions.any {
        (it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                it.permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                it.status.isGranted
    }

    LaunchedEffect(isLocationGranted) {
        if (isLocationGranted) {
            val serviceIntent = Intent(context, BackgroundSafetyService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }

    // --- UI Structure ---
    Scaffold(
        topBar = {
             CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when(currentRoute) {
                            BottomNavItem.Home.route -> "SafeTravel"
                            BottomNavItem.Map.route -> "Incident Map"
                            BottomNavItem.Pedestrian.route -> "Sensor"
                            BottomNavItem.Contacts.route -> "Contacts"
                            BottomNavItem.Profile.route -> "Profile"
                            else -> "SafeTravel"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    // AI Help Button
                    FilledTonalIconButton(
                        onClick = { rootNavController.navigate("ai_help") },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Assistant, // AI Sparkle icon
                            contentDescription = "AI Help",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Enhanced Notification Button
                    FilledTonalIconButton(
                        onClick = { rootNavController.navigate("sos_alerts") },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        BadgedBox(
                            badge = {
                                if (mainAppUiState.unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { 
                                        Text(
                                            "${mainAppUiState.unreadNotificationCount}",
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    }
                                }
                            }
                        ) {
                             Icon(
                                 Icons.Default.Notifications, 
                                 contentDescription = "Notifications",
                                 modifier = Modifier.size(24.dp)
                             )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Map,
                    BottomNavItem.Pedestrian,
                    BottomNavItem.Contacts,
                    BottomNavItem.Profile
                )
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
             // Pass the hoisted ViewModel to the button
             SosButton(viewModel = sosViewModel)
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onCreateTripClick = { rootNavController.navigate("create_trip") },
                    onTripClick = { tripId -> rootNavController.navigate("trip_management/$tripId") }
                )
            }
            composable(BottomNavItem.Map.route) {
                // Pass the hoisted ViewModel to the screen
                InTripScreen(
                    navController = rootNavController,
                    sosViewModel = sosViewModel
                )
            }
            composable(BottomNavItem.Pedestrian.route) {
                AccidentDetectionScreen()
            }
            composable(BottomNavItem.Contacts.route) {
                ContactsScreen(onNavigateUp = { /* No-op */ })
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onCreateTrip = { rootNavController.navigate("create_trip") },
                    onManageContacts = { bottomNavController.navigate(BottomNavItem.Contacts.route) },
                    onNavigateToSettings = { rootNavController.navigate("settings") },
                    onNavigateToSosAlerts = { rootNavController.navigate("sos_alerts") },
                    onNavigateToInTrip = { bottomNavController.navigate(BottomNavItem.Map.route) },
                    onNavigateToTripHistory = { tripId -> rootNavController.navigate("trip_history/$tripId") }
                )
            }
        }
    }
}
