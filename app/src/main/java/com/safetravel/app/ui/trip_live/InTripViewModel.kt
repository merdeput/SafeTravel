package com.safetravel.app.ui.trip_live

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.Coordinates
import com.safetravel.app.data.model.LocationData
import com.safetravel.app.data.repository.GeocodingService
import com.safetravel.app.data.repository.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class MarkerType {
    USER_REPORT,
    FRIEND_SOS,
    OTHER_USER_SOS,
    NORMAL
}

data class MapMarker(
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000).toLong(), // Simple unique ID
    val position: LatLng,
    val title: String,
    val type: MarkerType = MarkerType.NORMAL
) {
    fun getColor(): Color {
        return when (type) {
            MarkerType.USER_REPORT -> Color(0xFF2196F3) // Blue
            MarkerType.FRIEND_SOS -> Color(0xFFF44336) // Red
            MarkerType.OTHER_USER_SOS -> Color(0xFFFFEB3B) // Yellow
            MarkerType.NORMAL -> Color(0xFF9E9E9E) // Gray
        }
    }
}

data class InTripUiState(
    val currentLocation: LatLng? = null,
    val locationAccuracy: Float? = null,
    val markers: List<MapMarker> = emptyList(), // Filtered list
    val logMessages: List<String> = emptyList(),
    val isProcessingTap: Boolean = false,
    val cameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(LatLng(10.762622, 106.660172), 15f),
    val activeFilters: Set<MarkerType> = MarkerType.values().toSet(),
    val reports: List<MapMarker> = emptyList() // List of reports for the bottom sheet
)

@HiltViewModel
class InTripViewModel @Inject constructor(
    private val locationService: LocationService,
    private val geocodingService: GeocodingService,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InTripUiState())
    val uiState: StateFlow<InTripUiState> = _uiState.asStateFlow()
    
    // Master list of all markers
    private val _allMarkers = MutableStateFlow<List<MapMarker>>(emptyList())
    
    init {
        // Add some dummy markers for demonstration
        addDummyMarkers()
        
        // Update filtered markers and reports list
        viewModelScope.launch {
            combine(_allMarkers, _uiState.map { it.activeFilters }.distinctUntilChanged()) { markers, filters ->
                Pair(
                    markers.filter { it.type in filters }, // Filtered for Map
                    markers.filter { it.type == MarkerType.USER_REPORT } // Only User Reports for list
                )
            }.collect { (filteredMarkers, userReports) ->
                _uiState.update { 
                    it.copy(
                        markers = filteredMarkers,
                        reports = userReports
                    ) 
                }
            }
        }
    }
    
    fun toggleFilter(type: MarkerType) {
        _uiState.update { state ->
            val newFilters = if (state.activeFilters.contains(type)) {
                state.activeFilters - type
            } else {
                state.activeFilters + type
            }
            state.copy(activeFilters = newFilters)
        }
    }
    
    fun recenterCamera() {
        _uiState.value.currentLocation?.let { location ->
            _uiState.update {
                it.copy(cameraPosition = CameraPosition.fromLatLngZoom(location, 16f))
            }
        }
    }
    
    fun submitReport(message: String) {
        val location = _uiState.value.currentLocation ?: return
        
        // In a real app, send to API. Here, add to local list.
        val newReport = MapMarker(
            position = location,
            title = message,
            type = MarkerType.USER_REPORT
        )
        
        _allMarkers.value = _allMarkers.value + newReport
        
        // Optionally send to server
        sendLocationToServer(
            locationData = LocationData(
                type = "user_report",
                coordinates = Coordinates(location.latitude, location.longitude),
                timestamp = getCurrentTimestamp(),
                placeName = message
            ),
            logPrefix = "✓ Report Submitted"
        )
    }
    
    fun deleteReport(reportId: Long) {
        _allMarkers.update { currentMarkers ->
            currentMarkers.filterNot { it.id == reportId }
        }
    }
    
    fun resolveReport(reportId: Long) {
        // In a real app, update status on server. 
        // Here we just remove it effectively "resolving" it from the active map.
        // Or we could change its type/state if we had a more complex model.
        deleteReport(reportId)
    }

    private fun addDummyMarkers() {
        val baseLat = 10.762622
        val baseLng = 106.660172
        
        val dummyMarkers = listOf(
            MapMarker(position = LatLng(baseLat + 0.001, baseLng + 0.001), title = "Accident Reported", type = MarkerType.USER_REPORT),
            MapMarker(position = LatLng(baseLat - 0.002, baseLng - 0.001), title = "Friend SOS: John", type = MarkerType.FRIEND_SOS),
            MapMarker(position = LatLng(baseLat + 0.003, baseLng - 0.002), title = "SOS Alert nearby", type = MarkerType.OTHER_USER_SOS)
        )
        
        _allMarkers.value = _allMarkers.value + dummyMarkers
    }

    fun startLocationUpdates() {
        Log.d("InTripViewModel", "Starting location updates")
        viewModelScope.launch {
            locationService.getLocationUpdates(10000)
                .catch { e -> addLogMessage("✗ Location Error: ${e.message}") }
                .collect { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _uiState.update {
                        it.copy(
                            currentLocation = latLng,
                            locationAccuracy = location.accuracy
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

        _uiState.update { it.copy(isProcessingTap = true) }
        addLogMessage("... Getting location info")

        viewModelScope.launch {
            try {
                val placeName = geocodingService.getAddressFromLatLng(latLng)
                val newMarker = MapMarker(position = latLng, title = placeName, type = MarkerType.NORMAL)
                _allMarkers.value = _allMarkers.value + newMarker
                
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
                _uiState.update { it.copy(isProcessingTap = false) }
            }
        }
    }

    fun onPlaceSelected(latLng: LatLng, placeName: String) {
        val newMarker = MapMarker(position = latLng, title = placeName, type = MarkerType.NORMAL)
        _allMarkers.value = _allMarkers.value + newMarker
        
        _uiState.update { state ->
            state.copy(
                cameraPosition = CameraPosition.fromLatLngZoom(latLng, 15f)
            )
        }
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
        _uiState.update { state ->
            state.copy(logMessages = (state.logMessages + message).takeLast(5))
        }
    }

    private fun getCurrentTimestamp(): String {
        return Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
    }
}
