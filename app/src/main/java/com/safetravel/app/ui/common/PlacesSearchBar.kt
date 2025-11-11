package com.safetravel.app.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesSearchBar(
    onPlaceSelected: (LatLng, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showPredictions by remember { mutableStateOf(false) }

    // Initialize Places API
    val placesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(context, getApiKey(context))
        }
        Places.createClient(context)
    }

    val token = remember { AutocompleteSessionToken.newInstance() }

    Column(modifier = modifier) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (query.length >= 3) {
                    showPredictions = true
                    scope.launch {
                        searchPlaces(placesClient, query, token) { results ->
                            predictions = results
                        }
                    }
                } else {
                    predictions = emptyList()
                    showPredictions = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for a place...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Predictions list
        if (showPredictions && predictions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(predictions) { prediction ->
                        ListItem(
                            headlineContent = { Text(prediction.primaryText) },
                            supportingContent = { Text(prediction.secondaryText) },
                            modifier = Modifier.clickable {
                                isSearching = true
                                scope.launch {
                                    getPlaceDetails(
                                        placesClient,
                                        prediction.placeId,
                                        context
                                    ) { latLng, placeName ->
                                        if (latLng != null) {
                                            onPlaceSelected(latLng, placeName)
                                            searchQuery = placeName
                                            showPredictions = false
                                            predictions = emptyList()
                                        }
                                        isSearching = false
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

private fun searchPlaces(
    placesClient: PlacesClient,
    query: String,
    token: AutocompleteSessionToken,
    onResults: (List<PlacePrediction>) -> Unit
) {
    val request = FindAutocompletePredictionsRequest.builder()
        .setSessionToken(token)
        .setQuery(query)
        .build()

    placesClient.findAutocompletePredictions(request)
        .addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions.map { prediction ->
                PlacePrediction(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString()
                )
            }
            onResults(predictions)
        }
        .addOnFailureListener { exception ->
            onResults(emptyList())
        }
}

private fun getPlaceDetails(
    placesClient: PlacesClient,
    placeId: String,
    context: Context,
    onResult: (LatLng?, String) -> Unit
) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
    val request = FetchPlaceRequest.newInstance(placeId, placeFields)

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            val place = response.place
            val latLng = place.latLng
            val name = place.name ?: "Unknown Place"
            onResult(latLng, name)
        }
        .addOnFailureListener { exception ->
            Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            onResult(null, "")
        }
}

private fun getApiKey(context: Context): String {
    return try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    } catch (e: Exception) {
        ""
    }
}