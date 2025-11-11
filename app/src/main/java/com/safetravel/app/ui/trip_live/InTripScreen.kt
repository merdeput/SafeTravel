package com.safetravel.app.ui.trip_live

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.safetravel.app.ui.common.PlacesSearchBar

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InTripScreen(
    viewModel: InTripViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Get the UI state from the ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Permission state
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = uiState.cameraPosition
    }

    // Update camera when state changes (e.g., from search)
    LaunchedEffect(uiState.cameraPosition) {
        cameraPositionState.position = uiState.cameraPosition
    }


    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Start location updates when permissions are granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top section: Current location display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.currentLocation != null) {
                    Text("Lat: ${String.format("%.6f", uiState.currentLocation!!.latitude)}")
                    Text("Lng: ${String.format("%.6f", uiState.currentLocation!!.longitude)}")
                    uiState.locationAccuracy?.let {
                        Text("Accuracy: ${String.format("%.1f", it)}m")
                    }
                } else {
                    Text("Waiting for location...")
                }
            }
        }

        // Search bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            // We need to move PlacesSearchBar to ui/common
            // and have it call the ViewModel
            PlacesSearchBar(
                onPlaceSelected = { latLng, placeName ->
                    viewModel.onPlaceSelected(latLng, placeName)
                },
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Middle section: Google Map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = permissionsState.allPermissionsGranted),
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                onMapClick = { latLng ->
                    // Just tell the ViewModel what happened
                    viewModel.onMapClick(latLng)
                }
            ) {
                // Draw markers from the UI state
                uiState.markers.forEach { markerData ->
                    Marker(
                        state = MarkerState(position = markerData.position),
                        title = markerData.title,
                        snippet = "Lat: ${String.format("%.4f", markerData.position.latitude)}, " +
                                "Lng: ${String.format("%.4f", markerData.position.longitude)}"
                    )
                }
            }

            // Loading indicator when processing tap
            if (uiState.isProcessingTap) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize()
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Bottom section: Log area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "Activity Log:",
                    style = MaterialTheme.typography.labelMedium
                )
                uiState.logMessages.forEach { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Show permission dialog if needed
    if (!permissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location permission to function properly.") },
            confirmButton = {
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        )
    }
}