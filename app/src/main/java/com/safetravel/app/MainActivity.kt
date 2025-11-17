package com.safetravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safetravel.app.ui.createtrip.CreateTripScreen
import com.safetravel.app.ui.createtrip.CreateTripViewModel
import com.safetravel.app.ui.createtrip.LocationPickerScreen
import com.safetravel.app.ui.home.MainScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.profile.ContactsScreen
import com.safetravel.app.ui.profile.ProfileScreen
import com.safetravel.app.ui.sos.AiHelpScreen
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
                onManageContacts = { navController.navigate("contacts") } // Navigate to the new screen
            )
        }

        composable("contacts") {
            ContactsScreen(
                onNavigateUp = { navController.popBackStack() }
            )
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
                onStartTrip = { navController.navigate("main") { popUpTo("profile") { inclusive = true } } },
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

        composable("ai_help") {
            AiHelpScreen(
                onEmergencyStopped = { 
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }

        composable("main") {
            MainScreen(navController = navController)
        }
    }
}
