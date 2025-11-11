package com.safetravel.app.ui.trip_live

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.safetravel.app.data.model.Coordinates
import com.safetravel.app.data.model.LocationData
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.repository.GeocodingService
import com.safetravel.app.data.repository.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Represents a marker on the map
data class MapMarker(
    val position: LatLng,
    val title: String
)

// Represents the entire UI state for the map screen
data class InTripUiState(
    val currentLocation: LatLng? = null,
    val locationAccuracy: Float? = null,
    val markers: List<MapMarker> = emptyList(),
    val logMessages: List<String> = emptyList(),
    val isProcessingTap: Boolean = false,
    val cameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(LatLng(10.762622, 106.660172), 15f)
)

@HiltViewModel
class InTripViewModel @Inject constructor(
    private val locationService: LocationService,
    private val geocodingService: GeocodingService,
    private val apiService: ApiService
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow(InTripUiState())
    // Public read-only state for the UI to observe
    val uiState: StateFlow<InTripUiState> = _uiState.asStateFlow()

    fun startLocationUpdates() {
        Log.d("InTripViewModel", "Starting location updates")
        viewModelScope.launch {
            locationService.getLocationUpdates(10000)
                .catch { e ->
                    addLogMessage("✗ Location Error: ${e.message}")
                }
                .collect { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = latLng,
                        locationAccuracy = location.accuracy
                    )

                    // Only move camera if it's still at the default position
                    if (_uiState.value.cameraPosition.target.latitude == 10.762622) {
                        _uiState.value = _uiState.value.copy(
                            cameraPosition = CameraPosition.fromLatLngZoom(latLng, 15f)
                        )
                    }

                    sendLocationToServer(
                        locationData = LocationData(
                            type = "current_location",
                            coordinates = Coordinates(location.latitude, location.longitude),
                            accuracy = location.accuracy,
                            timestamp = getCurrentTimestamp()
                        ),
                        logPrefix = "✓ Current location"
                    )
                }
        }
    }

    fun onMapClick(latLng: LatLng) {
        if (_uiState.value.isProcessingTap) return

        _uiState.value = _uiState.value.copy(isProcessingTap = true)
        addLogMessage("... Getting location info")

        viewModelScope.launch {
            try {
                val placeName = geocodingService.getAddressFromLatLng(latLng)
                _uiState.value = _uiState.value.copy(
                    markers = _uiState.value.markers + MapMarker(latLng, placeName)
                )

                sendLocationToServer(
                    locationData = LocationData(
                        type = "tap_location",
                        coordinates = Coordinates(latLng.latitude, latLng.longitude),
                        timestamp = getCurrentTimestamp(),
                        placeName = placeName
                    ),
                    logPrefix = "✓ Tap: $placeName"
                )
            } catch (e: Exception) {
                addLogMessage("✗ Tap error: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isProcessingTap = false)
            }
        }
    }

    fun onPlaceSelected(latLng: LatLng, placeName: String) {
        _uiState.value = _uiState.value.copy(
            markers = _uiState.value.markers + MapMarker(latLng, placeName),
            cameraPosition = CameraPosition.fromLatLngZoom(latLng, 15f)
        )

        sendLocationToServer(
            locationData = LocationData(
                type = "search_location",
                coordinates = Coordinates(latLng.latitude, latLng.longitude),
                timestamp = getCurrentTimestamp(),
                placeName = placeName
            ),
            logPrefix = "✓ Search: $placeName"
        )
    }

    private fun sendLocationToServer(locationData: LocationData, logPrefix: String) {
        viewModelScope.launch {
            try {
                val response = apiService.sendLocation(locationData)
                if (response.isSuccessful) {
                    addLogMessage("$logPrefix sent")
                } else {
                    addLogMessage("✗ $logPrefix Failed: ${response.code()}")
                }
            } catch (e: Exception) {
                addLogMessage("✗ $logPrefix Error: ${e.message}")
            }
        }
    }

    private fun addLogMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            logMessages = (_uiState.value.logMessages + message).takeLast(5)
        )
    }

    private fun getCurrentTimestamp(): String {
        return Instant.now()
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
}