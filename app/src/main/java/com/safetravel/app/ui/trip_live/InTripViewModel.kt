package com.safetravel.app.ui.trip_live

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.GeocodingService
import com.safetravel.app.data.repository.IncidentRepository
import com.safetravel.app.data.repository.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MarkerType {
    USER_REPORT,
    FRIEND_SOS,
    OTHER_USER_SOS,
    NORMAL
}

data class MapMarker(
    val id: Long,
    val position: LatLng,
    val title: String,
    val description: String?, // Added description
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
    val reports: List<MapMarker> = emptyList(), // List of reports for the bottom sheet
    val selectedMarker: MapMarker? = null, // Track selected marker for route
    val routePoints: List<LatLng> = emptyList() // Store route points
)

@HiltViewModel
class InTripViewModel @Inject constructor(
    private val locationService: LocationService,
    private val geocodingService: GeocodingService,
    private val incidentRepository: IncidentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InTripUiState())
    val uiState: StateFlow<InTripUiState> = _uiState.asStateFlow()
    
    // Master list of all markers
    private val _allMarkers = MutableStateFlow<List<MapMarker>>(emptyList())
    
    init {
        // Start location updates immediately to get initial position
        viewModelScope.launch {
            startLocationUpdates()
        }
        
        // This combines all markers with the active filters to produce the displayed lists
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
    
    fun onMarkerClick(marker: MapMarker) {
        _uiState.update { it.copy(selectedMarker = marker) }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedMarker = null, routePoints = emptyList()) }
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
        
        viewModelScope.launch {
            val result = incidentRepository.createIncident(
                title = "User Report",
                description = message,
                category = "user_report",
                latitude = location.latitude,
                longitude = location.longitude,
                severity = 0 // Default severity
            )
            
            if (result.isSuccess) {
                addLogMessage("✓ Report Submitted")
                // Refresh incidents to show the new report
                loadIncidents(location.latitude, location.longitude)
            } else {
                 addLogMessage("✗ Report Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    fun deleteReport(reportId: Long) {
        viewModelScope.launch {
            val result = incidentRepository.deleteIncident(reportId)
            if (result.isSuccess) {
                addLogMessage("✓ Report Deleted")
                // Remove from local list immediately for better UX
                _allMarkers.update { currentMarkers ->
                    currentMarkers.filterNot { it.id == reportId }
                }
            } else {
                addLogMessage("✗ Delete Failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun loadIncidents(lat: Double, lng: Double) {
        viewModelScope.launch {
            val result = incidentRepository.getIncidents(lat, lng, 10.0) // 10km radius
            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    val currentUserId = authRepository.currentUser?.id?.toLong()

                    val markers = response.items.mapNotNull { itemWrapper ->
                        val item = itemWrapper.item

                        // Filter out resolved SOS
                        if (item.status.equals("resolved", ignoreCase = true)) {
                            return@mapNotNull null
                        }

                        val type = when (itemWrapper.priority) {
                            0 -> MarkerType.FRIEND_SOS // Red
                            1 -> MarkerType.OTHER_USER_SOS // Yellow
                            2 -> MarkerType.USER_REPORT // Blue
                            else -> MarkerType.NORMAL
                        }

                        // Filter out SOS from myself
                        val isSos = type == MarkerType.FRIEND_SOS || type == MarkerType.OTHER_USER_SOS
                        if (isSos && currentUserId != null && item.userId == currentUserId) {
                            return@mapNotNull null
                        }
                        
                        val title = when (type) {
                            MarkerType.FRIEND_SOS -> "SOS: ${item.user?.username ?: "Friend"}"
                            MarkerType.OTHER_USER_SOS -> "SOS Alert"
                            else -> item.title ?: "Incident"
                        }

                        val description = when (type) {
                             MarkerType.FRIEND_SOS -> item.message
                             MarkerType.OTHER_USER_SOS -> item.message
                             else -> item.description
                        }

                        MapMarker(
                            id = item.id,
                            position = LatLng(item.latitude, item.longitude),
                            title = title,
                            description = description,
                            type = type
                        )
                    }
                    _allMarkers.value = markers
                } else {
                    addLogMessage("No incidents found in the area")
                }
            } else {
                addLogMessage("✗ Failed to load incidents: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun startLocationUpdates() {
        Log.d("InTripViewModel", "Starting location updates")
        viewModelScope.launch {
            locationService.getLocationUpdates(10000) // Poll every 10 seconds
                .catch { e -> addLogMessage("✗ Location Error: ${e.message}") }
                .collect { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    val isFirstLocation = _uiState.value.currentLocation == null
                    
                    _uiState.update {
                        it.copy(
                            currentLocation = latLng,
                            locationAccuracy = location.accuracy
                        )
                    }
                    
                    // If this is the first location update, move the camera
                    if (isFirstLocation) {
                        recenterCamera()
                    }
                    
                    // Fetch incidents around the new location
                    loadIncidents(location.latitude, location.longitude)
                }
        }
    }

    fun onMapClick(latLng: LatLng) {
        // If a marker info card is shown, tapping on map should dismiss it
        if (_uiState.value.selectedMarker != null) {
            clearSelection()
        }
    }

    fun onPlaceSelected(latLng: LatLng, placeName: String) {
        _uiState.update { state ->
            state.copy(
                cameraPosition = CameraPosition.fromLatLngZoom(latLng, 15f)
            )
        }
        // Load incidents for the newly selected place
        loadIncidents(latLng.latitude, latLng.longitude)
    }

    private fun addLogMessage(message: String) {
        _uiState.update { state ->
            state.copy(logMessages = (state.logMessages + message).takeLast(5))
        }
    }
}
