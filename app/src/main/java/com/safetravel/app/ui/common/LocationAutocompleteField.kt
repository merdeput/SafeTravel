package com.safetravel.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.safetravel.app.ui.createtrip.LocationPickerViewModel

@Composable
fun LocationAutocompleteField(
    modifier: Modifier = Modifier,
    locationViewModel: LocationPickerViewModel, // Now expects a shared instance
    onPlaceSelected: (String) -> Unit
) {
    val uiState by locationViewModel.uiState.collectAsState()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = locationViewModel::onSearchQueryChange,
            label = { Text("Where are you going?") },
            modifier = Modifier.fillMaxWidth()
        )

        // Display autocomplete results
        if (uiState.autocompleteResults.isNotEmpty()) {
            uiState.autocompleteResults.forEach { result ->
                ListItem(
                    headlineContent = { Text(result.primaryText) },
                    supportingContent = { Text(result.secondaryText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { locationViewModel.onResultClick(result.placeId) }
                )
            }
        }

        // Once a place is selected, update the CreateTripViewModel
        LaunchedEffect(uiState.selectedPlaceName) {
            uiState.selectedPlaceName?.let {
                onPlaceSelected(it)
            }
        }
    }
}
