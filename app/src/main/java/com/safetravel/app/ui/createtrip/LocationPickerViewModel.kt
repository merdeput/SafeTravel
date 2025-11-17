package com.safetravel.app.ui.createtrip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AutocompleteResult(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

data class LocationPickerState(
    val searchQuery: String = "",
    val autocompleteResults: List<AutocompleteResult> = emptyList(),
    val selectedPlaceName: String? = null,
    val selectedPlaceLatLng: LatLng? = null,
    val error: String? = null
)

class LocationPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(application, "")
        }
        Places.createClient(application)
    }

    private val _uiState = MutableStateFlow(LocationPickerState())
    val uiState = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length > 2) {
            fetchAutocompleteResults(query)
        } else {
            _uiState.update { it.copy(autocompleteResults = emptyList()) }
        }
    }

    fun onPlaceSelected(latLng: LatLng, name: String) {
        _uiState.update {
            it.copy(
                selectedPlaceName = name,
                selectedPlaceLatLng = latLng,
                searchQuery = name, // Update search bar text as well
                autocompleteResults = emptyList()
            )
        }
    }

    private fun fetchAutocompleteResults(query: String) {
        viewModelScope.launch {
            val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                val results = response.autocompletePredictions.map {
                    AutocompleteResult(
                        placeId = it.placeId,
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString()
                    )
                }
                _uiState.update { it.copy(autocompleteResults = results, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not fetch autocomplete results.") }
            }
        }
    }

    fun onResultClick(placeId: String) {
        viewModelScope.launch {
            val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            try {
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                onPlaceSelected(place.latLng, place.name)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not fetch place details.") }
            }
        }
    }
}
