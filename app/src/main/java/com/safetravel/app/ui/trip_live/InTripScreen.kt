package com.safetravel.app.ui.trip_live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.safetravel.app.ui.common.PlacesSearchBar
import com.safetravel.app.ui.common.SosButton
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.common.SosState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InTripScreen(
    viewModel: InTripViewModel = hiltViewModel(),
    sosViewModel: SosButtonViewModel = hiltViewModel(), // Shared SOS ViewModel
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val sosUiState by sosViewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        uiState.cameraPosition?.let { position = it }
    }

    // Navigate to AI Help screen when SOS is triggered
    LaunchedEffect(sosUiState.sosState) {
        if (sosUiState.sosState is SosState.NavigateToAiHelp) {
            navController.navigate("ai_help")
            sosViewModel.onNavigatedToAiHelp() // Reset the state after navigation
        }
    }

    // Location Permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        } else {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        floatingActionButton = {
            SosButton(viewModel = sosViewModel) // Pass the shared ViewModel
        }
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = viewModel::onMapClick
                ) {
                    uiState.currentLocation?.let {
                        Marker(state = com.google.maps.android.compose.rememberMarkerState(position = it))
                    }
                    uiState.markers.forEach { markerData ->
                        Marker(
                            state = com.google.maps.android.compose.rememberMarkerState(position = markerData.position),
                            title = markerData.title
                        )
                    }
                }

                PlacesSearchBar(
                    onPlaceSelected = viewModel::onPlaceSelected
                )
            }

            // --- Restored UI for Location Data and Logs ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                // Location Info
                uiState.currentLocation?.let {
                    Row {
                        Text("Lat: ${String.format("%.6f", it.latitude)}", modifier = Modifier.weight(1f))
                        Text("Lng: ${String.format("%.6f", it.longitude)}", modifier = Modifier.weight(1f))
                    }
                    uiState.locationAccuracy?.let {
                        Text("Accuracy: ${String.format("%.1f", it)}m", style = MaterialTheme.typography.bodySmall)
                    }
                } ?: Text("Getting current location...")

                // Logs
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 100.dp)) {
                    items(uiState.logMessages) {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
