package com.safetravel.app.ui.trip_live

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.safetravel.app.ui.common.PlacesSearchBar
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.common.SosState
import kotlinx.coroutines.launch
import java.io.OutputStream

// Map Style JSON (Dark Mode)
private const val MAP_STYLE_JSON = """[{"elementType":"geometry","stylers":[{"color":"#242f3e"}]},{"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},{"elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},{"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},{"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#6b9a76"}]},{"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},{"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},{"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},{"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},{"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},{"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},{"featureType":"transit.station","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},{"featureType":"water","elementType":"labels.text.stroke","stylers":[{"color":"#17263c"}]}]"""

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InTripScreen(
    navController: NavController,
    viewModel: InTripViewModel = hiltViewModel(),
    sosViewModel: SosButtonViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val sosUiState by sosViewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reference to the underlying GoogleMap object to take snapshots
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    // --- Side Effects ---
    LaunchedEffect(uiState.cameraPosition) { cameraPositionState.position = uiState.cameraPosition }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(sosUiState.sosState) {
        if (sosUiState.sosState is SosState.NavigateToAiHelp) {
            navController.navigate("ai_help")
            sosViewModel.onNavigatedToAiHelp()
        }
    }

    // --- UI ---
    val scaffoldState = rememberBottomSheetScaffoldState()
    var reportMessage by remember { mutableStateOf("") }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                Text(
                    "Report Incident",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Report Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = reportMessage,
                        onValueChange = { reportMessage = it },
                        label = { Text("What happened?") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (reportMessage.isNotBlank()) {
                                viewModel.submitReport(reportMessage)
                                reportMessage = ""
                            }
                        },
                        enabled = reportMessage.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Submit Report")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Active Reports List
                Text(
                    "Your Active Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.reports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No active reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.reports) { report ->
                            ReportItem(
                                report = report,
                                onDelete = { viewModel.deleteReport(report.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val mapStyle = remember { MapStyleOptions(MAP_STYLE_JSON) }

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapStyleOptions = mapStyle
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapClick = viewModel::onMapClick
            ) {
                // Capture the GoogleMap instance
                MapEffect(Unit) { map ->
                    googleMap = map
                }

                // User's Current Location (Glowing Dot)
                uiState.currentLocation?.let { location ->
                    val userColor = Color(0xFF9E9E9E) // Grey
                    val glowingDot = remember(userColor) { createGlowingDotBitmap(userColor) }

                    Marker(
                        state = MarkerState(position = location),
                        title = "You are here",
                        icon = glowingDot,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        zIndex = 10f
                    )
                }

                // Other Markers
                uiState.markers.forEach { markerData ->
                    // Use title for the title field (which shows up on click) and description for snippet
                    if (markerData.type == MarkerType.NORMAL) {
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            snippet = markerData.description,
                            icon = BitmapDescriptorFactory.defaultMarker(),
                            onClick = {
                                viewModel.onMarkerClick(markerData)
                                false
                            }
                        )
                    } else {
                        val color = markerData.getColor()
                        val circleBitmap = remember(color) { createGlowingDotBitmap(color) }

                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            snippet = markerData.description,
                            icon = circleBitmap,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                            onClick = {
                                viewModel.onMarkerClick(markerData)
                                false
                            }
                        )
                    }
                }
            }

            // Top UI: Search & Simplified Filters
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlacesSearchBar(
                    onPlaceSelected = viewModel::onPlaceSelected,
                    modifier = Modifier.weight(1f)
                )

                var showFilterMenu by remember { mutableStateOf(false) }

                Box(modifier = Modifier.padding(end = 16.dp)) {
                    FilledTonalIconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Map")
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        MarkerType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when(type) {
                                            MarkerType.USER_REPORT -> "Reports"
                                            MarkerType.FRIEND_SOS -> "Friend SOS"
                                            MarkerType.OTHER_USER_SOS -> "Nearby SOS"
                                            MarkerType.NORMAL -> "Search Results"
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.toggleFilter(type)
                                },
                                leadingIcon = {
                                    val isSelected = uiState.activeFilters.contains(type)
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isProcessingTap) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Floating Buttons Stack
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 128.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("bluetooth_hearing") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = "SOS Hearing")
                }
                
                // Save Snapshot Button (Offline Map helper)
                FloatingActionButton(
                    onClick = {
                        googleMap?.snapshot { bitmap ->
                            if (bitmap != null) {
                                val saved = saveBitmapToGallery(context, bitmap, "SafeTravel_Map_${System.currentTimeMillis()}")
                                scope.launch {
                                    if (saved) {
                                        scaffoldState.snackbarHostState.showSnackbar("Map snapshot saved to Photos")
                                    } else {
                                        scaffoldState.snackbarHostState.showSnackbar("Failed to save snapshot")
                                    }
                                }
                            } else {
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar("Could not capture map")
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Save Offline Snapshot")
                }

                FloatingActionButton(
                    onClick = { viewModel.recenterCamera() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                }
            }
            
            // Marker Selection Info & Actions
            uiState.selectedMarker?.let { selected ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp, start = 16.dp, end = 16.dp) // Above bottom sheet peek
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Display Title first (as it contains Verification info), then description
                                Text(
                                    text = selected.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!selected.description.isNullOrEmpty()) {
                                    Text(
                                        text = selected.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                // Direct to Google Maps
                                val gmmIntentUri = Uri.parse("google.navigation:q=${selected.position.latitude},${selected.position.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    // Fallback if Google Maps app is not installed
                                    val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selected.position.latitude},${selected.position.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigate with Google Maps")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(
    report: MapMarker,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(report.getColor())
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Prioritize description
                Text(
                    text = report.description ?: report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Show title as secondary info if available
                if (!report.description.isNullOrEmpty() && report.title != report.description) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun createGlowingDotBitmap(color: Color): BitmapDescriptor {
    val coreSize = 30f
    val glowSize = 20f
    val totalSize = (coreSize + glowSize * 2).toInt()

    val bitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = totalSize / 2f

    val glowPaint = Paint().apply {
        this.color = color.copy(alpha = 0.6f).toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawCircle(center, center, (coreSize / 2f) + 5f, glowPaint)

    val borderPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, (coreSize / 2f) + 4f, borderPaint)

    val corePaint = Paint().apply {
        this.color = color.toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, coreSize / 2f, corePaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
    val filename = "${title}.jpg"
    var fos: OutputStream? = null
    var success = false
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SafeTravel")
            }
            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = context.contentResolver.openOutputStream(imageUri)
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    success = true
                }
            }
        } else {
             // For older versions, we could use standard file output to external storage
             // But for brevity and target SDK, focusing on modern approach.
             // If strictly needed, we would use Environment.getExternalStoragePublicDirectory...
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        fos?.close()
    }
    return success
}
