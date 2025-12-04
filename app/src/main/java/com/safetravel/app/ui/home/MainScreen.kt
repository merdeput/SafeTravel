package com.safetravel.app.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safetravel.app.ui.debug.SensorsScreen
import com.safetravel.app.ui.sos.AccidentDetectionScreen
import com.safetravel.app.ui.trip_live.InTripScreen
import com.safetravel.app.ui.trip_live.TripManagementScreen

// Screen routes for the main dashboard
sealed class Screen(val route: String, val title: String) {
    object InTrip : Screen("in_trip", "In Trip")
    // Changed route definition to include the parameter
    object TripManagement : Screen("trip_management/{circleId}", "Trip Mgmt") 
    object AccidentDetection : Screen("accident", "Accident Detection")
    object Sensors : Screen("sensors", "Sensors")
}

@Composable
fun MainScreen(navController: NavHostController) { // Pass NavController from parent
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
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
                InTripScreen(navController = navController)
            }
            
            // TripManagement needs circleId. 
            composable(
                route = "trip_management/{circleId}",
                arguments = listOf(navArgument("circleId") { type = NavType.IntType })
            ) {
                // The ViewModel will automatically get circleId from SavedStateHandle because it's in the arguments
                TripManagementScreen(
                    onEndTrip = { 
                        navController.navigate("profile") { 
                            popUpTo(0) 
                        }
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
    // Retrieve the circleId from the PARENT navController's back stack entry
    val parentEntry = try {
        parentNavController.getBackStackEntry("main/{circleId}")
    } catch (e: Exception) { null }
    val circleId = parentEntry?.arguments?.getInt("circleId")

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "In Trip") },
            label = { Text("Map") },
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
            icon = { Icon(Icons.Default.Group, contentDescription = "Trip Management") },
            label = { Text("Trip Mgmt") },
            // Use startsWith because the route now has a parameter
            selected = currentRoute?.startsWith("trip_management") == true, 
            onClick = { 
                // Construct the route dynamically with the ID
                val route = if (circleId != null) "trip_management/$circleId" else "trip_management/0"
                navigateToScreen(navController, route) 
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Build, contentDescription = "Debug") },
            label = { Text("Debug") },
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
