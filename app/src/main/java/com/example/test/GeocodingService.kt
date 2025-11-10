package com.example.test

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class GeocodingService(private val context: Context) {

    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getAddressFromLatLng(latLng: LatLng): String = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                // Try to build a readable address
                buildString {
                    // Get the feature name (e.g., building, park name)
                    address.featureName?.let {
                        if (it != address.subThoroughfare && it != address.thoroughfare) {
                            append(it)
                            append(", ")
                        }
                    }

                    // Street address
                    address.thoroughfare?.let {
                        append(it)
                        append(", ")
                    }

                    // District/Locality
                    address.subLocality?.let {
                        append(it)
                        append(", ")
                    } ?: address.locality?.let {
                        append(it)
                        append(", ")
                    }

                    // City
                    address.adminArea?.let {
                        append(it)
                    }
                }.takeIf { it.isNotBlank() } ?: "Unknown Location"
            } else {
                "Lat: ${String.format("%.4f", latLng.latitude)}, " +
                        "Lng: ${String.format("%.4f", latLng.longitude)}"
            }
        } catch (e: Exception) {
            "Lat: ${String.format("%.4f", latLng.latitude)}, " +
                    "Lng: ${String.format("%.4f", latLng.longitude)}"
        }
    }
}