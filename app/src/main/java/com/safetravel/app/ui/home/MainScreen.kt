package com.safetravel.app.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.safetravel.app.service.BackgroundSafetyService
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.debug.SensorsScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen
import com.safetravel.app.ui.trip_live.TripManagementScreen

// Screen routes for the main dashboard
sealed class Screen(val route: String, val title: String) {
    object InTrip : Screen("in_trip", "In Trip")
    object TripManagement : Screen("trip_management/{circleId}", "Trip Mgmt")
    object AccidentDetection : Screen("accident", "Accident Detection")
    object Sensors : Screen("sensors", "Sensors")
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: NavHostController) { // Pass NavController from parent
    val context = LocalContext.current
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define permissions to request
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

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // Start Background Service ONLY when Location permission is granted
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

    // Handle system back press to navigate to profile, ensuring it reloads
    BackHandler {
        navController.navigate("profile") { popUpTo("profile") { inclusive = true } }
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                navController = bottomNavController,
                currentRoute = currentRoute,
                parentNavController = navController
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.InTrip.route, // Start at the InTrip screen
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.InTrip.route) {
                // Pass the main NavController to InTripScreen
                val sosViewModel: SosButtonViewModel = hiltViewModel()
                InTripScreen(navController = navController, sosViewModel = sosViewModel)
            }

            composable(
                route = "trip_management/{circleId}",
                arguments = listOf(navArgument("circleId") { type = NavType.IntType })
            ) {
                TripManagementScreen(
                    onEndTrip = {
                        navController.navigate("profile") { popUpTo(0) }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile") { popUpTo("profile") { inclusive = true } }
                    }
                )
            }

            composable(Screen.AccidentDetection.route) {
                AccidentDetectionScreen()
            }
            composable(Screen.Sensors.route) {
                SensorsScreen()
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(
    navController: NavHostController,
    currentRoute: String?,
    parentNavController: NavHostController // Pass parent controller to retrieve ID
) {
    val parentEntry = try {
        parentNavController.getBackStackEntry("main/{circleId}")
    } catch (e: Exception) {
        null
    }
    val circleId = parentEntry?.arguments?.getInt("circleId")

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "In Trip") },
            label = { Text("Map") },
            selected = currentRoute == Screen.InTrip.route,
            onClick = { navigateToScreen(navController, Screen.InTrip.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "Accident Detection") },
            label = { Text("Detection") },
            selected = currentRoute == Screen.AccidentDetection.route,
            onClick = { navigateToScreen(navController, Screen.AccidentDetection.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Group, contentDescription = "Trip Management") },
            label = { Text("Trip Mgmt") },
            // Use startsWith because the route now has a parameter
            selected = currentRoute?.startsWith("trip_management") == true,
            onClick = {
                // Construct the route dynamically with the ID
                val route = if (circleId != null) "trip_management/$circleId" else "trip_management/0"
                navigateToScreen(navController, route)
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Build, contentDescription = "Debug") },
            label = { Text("Debug") },
            selected = currentRoute == Screen.Sensors.route,
            onClick = { navigateToScreen(navController, Screen.Sensors.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

private fun navigateToScreen(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
