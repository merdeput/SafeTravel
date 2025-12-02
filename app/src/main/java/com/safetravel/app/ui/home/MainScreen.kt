package com.safetravel.app.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.safetravel.app.service.BackgroundSafetyService
import com.safetravel.app.ui.debug.SensorsScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen

// Screen routes for the main dashboard
sealed class Screen(val route: String, val title: String) {
    object InTrip : Screen("in_trip", "In Trip")
    object Sensors : Screen("sensors", "Sensors")
    object AccidentDetection : Screen("accident", "Accident Detection")
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
    // (Required for FOREGROUND_SERVICE_LOCATION on Android 14+)
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

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController = bottomNavController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.InTrip.route, // Start at the InTrip screen
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.InTrip.route) {
                // Pass the main NavController to InTripScreen
                InTripScreen(navController = navController)
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
private fun AppBottomNavigation(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "In Trip") },
            label = { Text("In Trip") },
            selected = currentRoute == Screen.InTrip.route,
            onClick = { navigateToScreen(navController, Screen.InTrip.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "Accident Detection") },
            label = { Text("Detection") },
            selected = currentRoute == Screen.AccidentDetection.route,
            onClick = { navigateToScreen(navController, Screen.AccidentDetection.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Sensors") },
            label = { Text("Sensors") },
            selected = currentRoute == Screen.Sensors.route,
            onClick = { navigateToScreen(navController, Screen.Sensors.route) }
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
