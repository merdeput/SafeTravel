package com.example.test

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.test.ui.theme.TestTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestTheme {
                LocationMapScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationMapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permission state
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Location state
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationAccuracy by remember { mutableStateOf<Float?>(null) }
    var markers by remember { mutableStateOf<List<MarkerData>>(emptyList()) }
    var logMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProcessingTap by remember { mutableStateOf(false) }

    // Services
    val locationService = remember { LocationService(context) }
    val geocodingService = remember { GeocodingService(context) }

    // Camera position
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(10.762622, 106.660172), // Default to HCMC
            15f
        )
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Location updates every 10 seconds
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            locationService.getLocationUpdates(10000).collect { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                currentLocation = latLng
                locationAccuracy = location.accuracy

                // Move camera to current location (only first time)
                if (cameraPositionState.position.target.latitude == 10.762622) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                }

                // Send to server
                scope.launch {
                    try {
                        val locationData = LocationData(
                            type = "current_location",
                            coordinates = Coordinates(location.latitude, location.longitude),
                            accuracy = location.accuracy,
                            timestamp = getCurrentTimestamp()
                        )
                        val response = ApiClient.apiService.sendLocation(locationData)
                        if (response.isSuccessful) {
                            logMessages = logMessages + "✓ Current location sent"
                        } else {
                            logMessages = logMessages + "✗ Failed: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        logMessages = logMessages + "✗ Error: ${e.message}"
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    if (currentLocation != null) {
                        Text("Lat: ${String.format("%.6f", currentLocation!!.latitude)}")
                        Text("Lng: ${String.format("%.6f", currentLocation!!.longitude)}")
                        locationAccuracy?.let {
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
                PlacesSearchBar(
                    onPlaceSelected = { latLng, placeName ->
                        // Add marker
                        markers = markers + MarkerData(latLng, placeName)

                        // Move camera to selected place
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)

                        // Send to server
                        scope.launch {
                            try {
                                val locationData = LocationData(
                                    type = "search_location",
                                    coordinates = Coordinates(latLng.latitude, latLng.longitude),
                                    timestamp = getCurrentTimestamp(),
                                    placeName = placeName
                                )
                                val response = ApiClient.apiService.sendLocation(locationData)
                                if (response.isSuccessful) {
                                    logMessages = logMessages + "✓ Search: $placeName"
                                    Toast.makeText(context, "Location sent: $placeName", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                logMessages = logMessages + "✗ Search error: ${e.message}"
                            }
                        }
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
                        if (!isProcessingTap) {
                            isProcessingTap = true

                            // Show immediate feedback
                            Toast.makeText(context, "Getting location info...", Toast.LENGTH_SHORT).show()

                            scope.launch {
                                try {
                                    // Get address from coordinates (reverse geocoding)
                                    val placeName = geocodingService.getAddressFromLatLng(latLng)

                                    // Add marker with place name
                                    markers = markers + MarkerData(latLng, placeName)

                                    // Send to server
                                    val locationData = LocationData(
                                        type = "tap_location",
                                        coordinates = Coordinates(latLng.latitude, latLng.longitude),
                                        timestamp = getCurrentTimestamp(),
                                        placeName = placeName
                                    )
                                    val response = ApiClient.apiService.sendLocation(locationData)

                                    if (response.isSuccessful) {
                                        logMessages = logMessages + "✓ Tap: $placeName"
                                        Toast.makeText(context, "Location sent: $placeName", Toast.LENGTH_SHORT).show()
                                    } else {
                                        logMessages = logMessages + "✗ Failed: ${response.code()}"
                                    }
                                } catch (e: Exception) {
                                    logMessages = logMessages + "✗ Tap error: ${e.message}"
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isProcessingTap = false
                                }
                            }
                        }
                    }
                ) {
                    // Draw markers
                    markers.forEach { markerData ->
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            snippet = "Lat: ${String.format("%.4f", markerData.position.latitude)}, " +
                                    "Lng: ${String.format("%.4f", markerData.position.longitude)}"
                        )
                    }
                }

                // Loading indicator when processing tap
                if (isProcessingTap) {
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
                    logMessages.takeLast(3).forEach { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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

data class MarkerData(
    val position: LatLng,
    val title: String
)

fun getCurrentTimestamp(): String {
    return Instant.now()
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_INSTANT)
}