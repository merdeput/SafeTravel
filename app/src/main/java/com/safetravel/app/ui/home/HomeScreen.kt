package com.safetravel.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.data.model.NewsWeatherResponse
import com.safetravel.app.data.model.ProvinceData
import com.safetravel.app.data.model.TravelAdvice
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.model.WeatherForecast
import com.safetravel.app.ui.MainAppViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    mainAppViewModel: MainAppViewModel = hiltViewModel(), // Inject MainAppViewModel
    onCreateTripClick: () -> Unit,
    onTripClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val mainAppState by mainAppViewModel.uiState.collectAsState() // Observe MainApp state

    // Refresh data when screen appears
    LaunchedEffect(Unit) {
        viewModel.checkActiveTrip()
        mainAppViewModel.fetchActiveTripReport() // Trigger report fetch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                item {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.userName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (uiState.currentTrip != null) {
                    item {
                        ActiveTripDashboard(
                            trip = uiState.currentTrip!!,
                            newsWeather = mainAppState.activeTripWeatherReport ?: uiState.newsWeather, // Use active trip report if available
                            isLoadingReport = mainAppState.isLoadingReport,
                            onManageTrip = { onTripClick(uiState.currentTrip!!.id) }
                        )
                    }
                } else {
                    item {
                        NoTripState(onCreateTripClick = onCreateTripClick)
                    }
                }
            }
        }
    }
}

@Composable
fun NoTripState(onCreateTripClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Active Trip",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Plan your next adventure with AI safety insights and real-time monitoring.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCreateTripClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Trip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActiveTripDashboard(
    trip: TripDTO, 
    newsWeather: NewsWeatherResponse?,
    isLoadingReport: Boolean,
    onManageTrip: () -> Unit
) {
    val provinceData = newsWeather?.provinces?.firstOrNull()
    val weather = provinceData?.weatherForecast?.firstOrNull()
    
    // Safety Score Logic
    val score = provinceData?.score ?: 95 // Default or from mock
    val (scoreColor, scoreText, scoreIcon) = when {
        score > 75 -> Triple(Color(0xFF4CAF50), "Safe", Icons.Outlined.CheckCircle) // Green
        score >= 50 -> Triple(Color(0xFFFFC107), "Caution", Icons.Outlined.WarningAmber) // Amber
        else -> Triple(Color(0xFFF44336), "Dangerous", Icons.Outlined.Dangerous) // Red
    }
    
    // Parse temperature
    val tempDisplay = weather?.temperature?.split("-")?.get(0)?.replace("°C", "")?.trim()?.plus("°") 
        ?: weather?.temperature 
        ?: "24°"
    
    val conditionDisplay = weather?.condition?.split(".")?.get(0) ?: "Sunny"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Main Trip Card with Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CURRENT TRIP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    trip.destination,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny, // Placeholder icon
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Started: ${trip.startDate.split("T")[0]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "Safety Score",
                value = if (isLoadingReport && provinceData == null) "--" else "${score}/100", // Added /100
                subtitle = if (isLoadingReport && provinceData == null) "Loading..." else scoreText,
                modifier = Modifier.weight(1f),
                icon = scoreIcon,
                color = if (isLoadingReport && provinceData == null) Color.Gray else scoreColor
            )
            StatCard(
                title = "Weather",
                value = if (isLoadingReport && provinceData == null) "--" else tempDisplay,
                subtitle = if (isLoadingReport && provinceData == null) "Loading..." else conditionDisplay,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WbSunny,
                color = if (isLoadingReport && provinceData == null) Color.Gray else Color(0xFFFFB300)
            )
        }

        Button(
            onClick = onManageTrip,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
             colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("Manage Trip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        // News & Summary Card
        if (isLoadingReport) {
            LoadingCard()
        } else if (provinceData != null) {
            NewsAndWeatherCard(provinceData, scoreColor)
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().height(150.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Fetching AI Insights...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NewsAndWeatherCard(provinceData: ProvinceData, accentColor: Color) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Executive Summary
            if (!provinceData.executiveSummary.isNullOrBlank()) {
                ContainerBox(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ) {
                   Column {
                       Text(
                           text = "EXECUTIVE SUMMARY",
                           style = MaterialTheme.typography.labelSmall,
                           fontWeight = FontWeight.Bold,
                           color = MaterialTheme.colorScheme.tertiary
                       )
                       Spacer(modifier = Modifier.height(8.dp))
                       Text(
                           text = provinceData.executiveSummary,
                           style = MaterialTheme.typography.bodyMedium,
                           color = MaterialTheme.colorScheme.onSurface
                       )
                   }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Weather Forecast
            if (!provinceData.weatherForecast.isNullOrEmpty()) {
                Text(
                     text = "Forecast",
                     style = MaterialTheme.typography.titleSmall,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(provinceData.weatherForecast) { weather ->
                         WeatherForecastCard(weather)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Travel Advice
            if (!provinceData.travelAdvice.isNullOrEmpty()) {
                Text(
                     text = "Travel Advice",
                     style = MaterialTheme.typography.titleSmall,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                provinceData.travelAdvice.forEach { advice ->
                    AdviceCard(advice)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WeatherForecastCard(weather: WeatherForecast) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.width(100.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = weather.date ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(text = weather.temperature ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = weather.condition ?: "", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AdviceCard(advice: TravelAdvice) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = if (advice.category == "An toàn") Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = advice.category ?: "General",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = advice.advice ?: "",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ContainerBox(
    color: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
