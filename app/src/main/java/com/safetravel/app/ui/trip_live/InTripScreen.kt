package com.safetravel.app.ui.trip_live

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.maps.android.compose.*
import com.safetravel.app.ui.common.PlacesSearchBar
import com.safetravel.app.ui.common.SosButton
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.common.SosState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InTripScreen(
    navController: NavController,
    viewModel: InTripViewModel = hiltViewModel(),
    sosViewModel: SosButtonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sosUiState by sosViewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    // --- Side Effects ---
    LaunchedEffect(uiState.cameraPosition) { cameraPositionState.position = uiState.cameraPosition }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(sosUiState.sosState) {
        if (sosUiState.sosState is SosState.NavigateToAiHelp) {
            navController.navigate("ai_help")
            sosViewModel.onNavigatedToAiHelp()
        }
    }

    // --- UI ---
    Scaffold(
        floatingActionButton = { SosButton(viewModel = sosViewModel) }
    ) {
        Column(
            modifier = Modifier.padding(it).fillMaxSize()
        ) {
            // Top section: Google Map & Search
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationPermissions.allPermissionsGranted),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                    onMapClick = viewModel::onMapClick
                ) {
                    uiState.markers.forEach { markerData ->
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title
                        )
                    }
                }
                PlacesSearchBar(onPlaceSelected = viewModel::onPlaceSelected)
                if (uiState.isProcessingTap) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Bottom section: Location Data and Logs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val location = uiState.currentLocation // Capture the location in a stable local variable
                if (location != null) {
                    Text("Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}")
                    uiState.locationAccuracy?.let { Text("Accuracy: ${String.format("%.1f", it)}m") }
                } else {
                    Text("Waiting for location...")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Activity Log:", style = MaterialTheme.typography.labelMedium)
                Column(modifier = Modifier.heightIn(max = 100.dp)) {
                    uiState.logMessages.forEach { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
