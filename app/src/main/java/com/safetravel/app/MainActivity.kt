package com.safetravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safetravel.app.ui.createtrip.CreateTripScreen
import com.safetravel.app.ui.createtrip.LocationPickerScreen
import com.safetravel.app.ui.home.MainScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.profile.ProfileScreen
import com.safetravel.app.ui.theme.TestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            TestTheme {
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
                onLoginSuccess = {
                    navController.navigate("profile") {
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

        composable("profile") {
            ProfileScreen(
                onCreateTrip = { navController.navigate("create_trip") },
                onTripClick = { /* TODO */ }
            )
        }

        composable("create_trip") { backStackEntry ->
            // Get the selected location from LocationPickerScreen if available
            val selectedLocation = backStackEntry.savedStateHandle.get<String>("selected_location")

            CreateTripScreen(
                onStartTrip = {
                    navController.navigate("main") {
                        popUpTo("profile") { inclusive = true }
                    }
                },
                onNavigateToLocationPicker = { navController.navigate("location_picker") },
                initialLocation = selectedLocation
            )
        }

        composable("location_picker") {
            LocationPickerScreen(
                onLocationSelected = { selectedLocation ->
                    // Save the selected location to the previous screen's savedStateHandle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_location", selectedLocation)
                    navController.popBackStack()
                },
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable("main") {
            MainScreen()
        }
    }
}