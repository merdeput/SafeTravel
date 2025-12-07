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
import com.safetravel.app.ui.createtrip.CreateTripScreen
import com.safetravel.app.ui.createtrip.CreateTripViewModel
import com.safetravel.app.ui.createtrip.LocationPickerScreen
import com.safetravel.app.ui.home.MainScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.profile.ContactsScreen
import com.safetravel.app.ui.profile.ProfileScreen
import com.safetravel.app.ui.profile.SettingsScreen
import com.safetravel.app.ui.sos.AiHelpScreen
import com.safetravel.app.ui.sos.SosAlertsScreen
import com.safetravel.app.ui.theme.BeeTheme
import com.safetravel.app.ui.triphistory.TripHistoryScreen

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

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("profile") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistrationSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                onCreateTrip = { navController.navigate("create_trip") },
                onManageContacts = { navController.navigate("contacts") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToSosAlerts = { navController.navigate("sos_alerts") },
                onNavigateToInTrip = { circleId -> 
                    navController.navigate("main/$circleId") 
                },
                onNavigateToTripHistory = { tripId ->
                    navController.navigate("trip_history/$tripId")
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

        composable("contacts") {
            ContactsScreen(onNavigateUp = { navController.popBackStack() })
        }

        composable("sos_alerts") {
            SosAlertsScreen(onNavigateUp = { navController.popBackStack() })
        }

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

            CreateTripScreen(
                viewModel = createTripViewModel,
                onStartTrip = { 
                    createTripViewModel.onStartTripClick() 
                },
                onNavigateToLocationPicker = { navController.navigate("location_picker") }
            )
            
            val uiState by createTripViewModel.uiState.collectAsState()
            
            LaunchedEffect(uiState.createdCircleId) {
                uiState.createdCircleId?.let { circleId ->
                    navController.navigate("main/$circleId") { 
                        popUpTo("create_trip") { inclusive = true } 
                    }
                    createTripViewModel.onTripCreationNavigated()
                }
            }
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

        composable("ai_help") {
            AiHelpScreen(
                onEmergencyStopped = { navController.popBackStack("main", inclusive = false) }
            )
        }

        composable(
            route = "main/{circleId}",
            arguments = listOf(navArgument("circleId") { type = NavType.IntType })
        ) {
            MainScreen(navController = navController)
        }
    }
}
