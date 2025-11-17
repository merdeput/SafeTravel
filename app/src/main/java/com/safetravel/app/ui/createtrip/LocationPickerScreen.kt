package com.safetravel.app.ui.createtrip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.safetravel.app.ui.common.PlacesSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    viewModel: LocationPickerViewModel = viewModel(),
    onLocationSelected: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(10.762622, 106.660172), 10f) // Default to HCMC
    }

    // Move camera when a place is selected
    LaunchedEffect(uiState.selectedPlaceLatLng) {
        uiState.selectedPlaceLatLng?.let {
            cameraPositionState.animate(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Destination") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { uiState.selectedPlaceName?.let { onLocationSelected(it) } },
                        enabled = uiState.selectedPlaceName != null
                    ) {
                        Text("Confirm")
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it).fillMaxSize()) {
            // Search Bar
            PlacesSearchBar(
                onPlaceSelected = { latLng, name ->
                    viewModel.onPlaceSelected(latLng, name)
                },
                modifier = Modifier.padding(8.dp)
            )

            // Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                uiState.selectedPlaceLatLng?.let {
                    Marker(state = com.google.maps.android.compose.rememberMarkerState(position = it))
                }
            }
        }
    }
}
