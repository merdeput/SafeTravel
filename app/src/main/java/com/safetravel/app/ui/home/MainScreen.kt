package com.safetravel.app.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safetravel.app.ui.debug.SensorsScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen

// Screen routes for the main dashboard
sealed class Screen(val route: String, val title: String) {
    object InTrip : Screen("in_trip", "In Trip")
    object Sensors : Screen("sensors", "Sensors")
    object AccidentDetection : Screen("accident", "Accident Detection")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.InTrip.route, // Start at the InTrip screen
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.InTrip.route) {
                InTripScreen()
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
