
package com.safetravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safetravel.app.ui.home.MainScreen
import com.safetravel.app.ui.login.LoginScreen
import com.safetravel.app.ui.login.RegisterScreen
import com.safetravel.app.ui.theme.TestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistrationSuccess = {
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                onNavigateToLogin = {
                    navController.navigateUp()
                }
            )
        }

        composable("main") {
            MainScreen()
        }
    }
}
