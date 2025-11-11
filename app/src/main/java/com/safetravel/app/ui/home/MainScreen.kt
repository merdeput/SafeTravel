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
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen

// Define screen routes
sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object InTrip : Screen("in_trip", "In Trip")
    object Sensors : Screen("sensors", "Sensors")
    object AccidentDetection : Screen("accident", "Accident Detection")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Screens that should show the bottom navigation bar
    val bottomNavRoutes = setOf(Screen.InTrip.route, Screen.AccidentDetection.route, Screen.Sensors.route)

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                AppBottomNavigation(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route, // Start at the login screen
            modifier = Modifier.padding(paddingValues)
        ) {
            // Composable for "Login"
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        // On success, navigate to the main screen and clear the back stack
                        navController.navigate(Screen.InTrip.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Composable for "In Trip" (Map Screen)
            composable(Screen.InTrip.route) {
                InTripScreen() // Our new, clean, refactored screen
            }

            // Composable for "Accident Detection"
            composable(Screen.AccidentDetection.route) {
                // We'll move PedestrianAccidentScreen.kt to this folder
                // and call it here.
                AccidentDetectionScreen()
            }

            // Composable for "Sensors"
            composable(Screen.Sensors.route) {
                // We'll move SensorsScreen.kt to this folder
                // and call it here.
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
        // Pop up to the start destination of the graph to avoid building up a large back stack
        // on the bottom nav.
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
