package com.safetravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import com.safetravel.app.ui.MainAppScreen
import com.safetravel.app.ui.createtrip.CreateTripScreen
import com.safetravel.app.ui.createtrip.CreateTripViewModel
import com.safetravel.app.ui.createtrip.LocationPickerScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.profile.SettingsScreen
import com.safetravel.app.ui.sos.AiHelpScreen
import com.safetravel.app.ui.sos.SosAlertsScreen
import com.safetravel.app.ui.theme.BeeTheme
import com.safetravel.app.ui.triphistory.TripHistoryScreen
import com.safetravel.app.ui.trip_live.TripManagementScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            BeeTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Start at Login (Auth Flow) - In a real app with persistence, we'd check token here
    NavHost(navController = navController, startDestination = "login") {

        // --- Auth Flow ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = { 
                    navController.navigate("main_app") { 
                        popUpTo("login") { inclusive = true } 
                    } 
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistrationSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Main App Flow (Bottom Navigation) ---
        composable("main_app") {
            MainAppScreen(rootNavController = navController)
        }

        // --- Full Screen Flows (Overlaying Bottom Bar) ---
        
        composable("create_trip") {
            val createTripViewModel: CreateTripViewModel = hiltViewModel()
            val backStackEntry = navController.currentBackStackEntry

            LaunchedEffect(backStackEntry) {
                val savedStateHandle = backStackEntry?.savedStateHandle
                savedStateHandle?.get<String>("selected_location_name")?.let {
                    createTripViewModel.onWhereChange(it)
                    savedStateHandle.remove<String>("selected_location_name")
                }
            }
            
            val uiState by createTripViewModel.uiState.collectAsState()
            
            // Navigate to MainApp (Home/Map) when trip is created
            LaunchedEffect(uiState.createdCircleId) {
                uiState.createdCircleId?.let { circleId ->
                    // Go back to main app, potentially clearing create_trip from stack
                    navController.navigate("main_app") {
                        popUpTo("create_trip") { inclusive = true }
                    }
                    createTripViewModel.onTripCreationNavigated()
                }
            }

            CreateTripScreen(
                viewModel = createTripViewModel,
                onStartTrip = { createTripViewModel.onStartTripClick() },
                onNavigateToLocationPicker = { navController.navigate("location_picker") }
            )
        }

        composable("location_picker") {
            LocationPickerScreen(
                onLocationSelected = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_location_name", it)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "trip_management/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.IntType })
        ) {
            TripManagementScreen(
                onEndTrip = {
                    navController.navigate("main_app") { popUpTo("main_app") { inclusive = true } }
                },
                onNavigateToProfile = {
                    // Just pop back, or navigate to main_app -> profile?
                    // Pop back usually works if we came from Home
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "trip_history/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.IntType })
        ) {
            TripHistoryScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                onNavigateUp = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // Use SosAlertsScreen for "notifications" route as requested
        composable("notifications") {
            SosAlertsScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("sos_alerts") {
            SosAlertsScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable("ai_help") {
            AiHelpScreen(
                onEmergencyStopped = { 
                    navController.popBackStack("main_app", inclusive = false) 
                }
            )
        }
    }
}
