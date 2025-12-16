package com.safetravel.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safetravel.app.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import com.safetravel.app.ui.MainAppScreen
import com.safetravel.app.ui.createtrip.CreateTripScreen
import com.safetravel.app.ui.createtrip.CreateTripViewModel
import com.safetravel.app.ui.createtrip.LocationPickerScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.profile.EmergencyInfoScreen
import com.safetravel.app.ui.profile.SettingsScreen
import com.safetravel.app.ui.sos.AiHelpScreen
import com.safetravel.app.ui.sos.BluetoothHearingScreen
import com.safetravel.app.ui.sos.SosAlertsScreen
import com.safetravel.app.ui.theme.BeeTheme
import com.safetravel.app.ui.triphistory.TripHistoryScreen
import com.safetravel.app.ui.trip_live.TripManagementScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            BeeTheme {
                // Determine start destination based on whether a token exists
                val startDestination = if (authRepository.currentToken != null) "main_app" else "login"
                AppNavigation(startDestination = startDestination)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Handle deep links from Notifications (e.g. Bluetooth Hearing)
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val intent = activity?.intent
        
        // Check for navigation route extra
        val route = intent?.getStringExtra("navigation_route")
        handleNavigationRoute(route, navController, intent)
    }
    
    // Also listen for new intents if the activity is already running
    DisposableEffect(Unit) {
        val listener = Consumer<Intent> { intent ->
            val route = intent.getStringExtra("navigation_route")
            handleNavigationRoute(route, navController, intent)
        }
        val activity = context as? ComponentActivity
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

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
                onNavigateToEmergencyInfo = { navController.navigate("emergency_info") },
                onLogout = {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        
        composable("emergency_info") {
            EmergencyInfoScreen(
                onNavigateUp = { navController.popBackStack() }
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
        
        composable("bluetooth_hearing") {
            BluetoothHearingScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}

private fun handleNavigationRoute(
    route: String?,
    navController: NavHostController,
    intent: Intent?
) {
    when (route) {
        "bluetooth_hearing", "ai_help" -> {
            navController.navigate(route) {
                popUpTo("main_app") { saveState = true }
                launchSingleTop = true
            }
            intent?.removeExtra("navigation_route")
        }
    }
}
