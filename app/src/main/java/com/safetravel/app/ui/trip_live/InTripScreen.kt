package com.safetravel.app.ui.trip_live

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.safetravel.app.ui.common.PlacesSearchBar
import com.safetravel.app.ui.common.SosButtonViewModel
import com.safetravel.app.ui.common.SosState

private const val MAP_STYLE_JSON = """[{"elementType":"geometry","stylers":[{"color":"#242f3e"}]},{"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},{"elementType":"labels.text.fill","stylers":[{"color":"#746855"}]},{"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#263c3f"}]},{"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#6b9a76"}]},{"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},{"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#212a37"}]},{"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#9ca5b3"}]},{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#746855"}]},{"featureType":"road.highway","elementType":"geometry.stroke","stylers":[{"color":"#1f2835"}]},{"featureType":"road.highway","elementType":"labels.text.fill","stylers":[{"color":"#f3d19c"}]},{"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},{"featureType":"transit.station","elementType":"labels.text.fill","stylers":[{"color":"#d59563"}]},{"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#515c6d"}]},{"featureType":"water","elementType":"labels.text.stroke","stylers":[{"color":"#17263c"}]}]"""

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InTripScreen(
    navController: NavController,
    viewModel: InTripViewModel = hiltViewModel(),
    sosViewModel: SosButtonViewModel // Hoisted from MainAppScreen
) {
    val uiState by viewModel.uiState.collectAsState()
    val sosUiState by sosViewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState { position = uiState.cameraPosition }

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

    // This LaunchedEffect now correctly observes the hoisted ViewModel
    LaunchedEffect(sosUiState.sosState) {
        if (sosUiState.sosState is SosState.NavigateToAiHelp) {
            navController.navigate("ai_help")
            sosViewModel.onNavigatedToAiHelp() // Reset the state
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
                            .heightIn(max = 250.dp), // Limit height to allow scrolling
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.reports) { report ->
                            ReportItem(
                                report = report,
                                onResolve = { viewModel.resolveReport(report.id) },
                                onDelete = { viewModel.deleteReport(report.id) }
                            )
                        }
                        // Spacer to lift the last item above the global FAB
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
                    mapStyleOptions = mapStyle // Apply the dark style
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapClick = viewModel::onMapClick
            ) {
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
                    if (markerData.type == MarkerType.NORMAL) {
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            icon = BitmapDescriptorFactory.defaultMarker()
                        )
                    } else {
                        val color = markerData.getColor()
                        val circleBitmap = remember(color) { createGlowingDotBitmap(color) }
    
                        Marker(
                            state = MarkerState(position = markerData.position),
                            title = markerData.title,
                            icon = circleBitmap,
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
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
                                    // Don't close menu on click to allow multiple selections
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
                                        // Add a spacer to keep alignment consistent
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

            // Recenter Camera Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 140.dp) 
            ) {
                FloatingActionButton(
                    onClick = { viewModel.recenterCamera() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter Map"
                    )
                }
            }
        }
    }
}

@Composable
fun ReportItem(
    report: MapMarker,
    onResolve: () -> Unit,
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
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onResolve) {
                Icon(Icons.Default.Check, contentDescription = "Resolve", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun createGlowingDotBitmap(color: Color): com.google.android.gms.maps.model.BitmapDescriptor {
    val coreSize = 30f // Diameter of the solid core
    val glowSize = 20f // Extra radius for the glow
    val totalSize = (coreSize + glowSize * 2).toInt()

    val bitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = totalSize / 2f

    // 1. Draw Glow (blurred circle)
    val glowPaint = Paint().apply {
        this.color = color.copy(alpha = 0.6f).toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawCircle(center, center, (coreSize / 2f) + 5f, glowPaint)

    // 2. Draw White Border
    val borderPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, (coreSize / 2f) + 4f, borderPaint)

    // 3. Draw Solid Core
    val corePaint = Paint().apply {
        this.color = color.toArgb()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, coreSize / 2f, corePaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
