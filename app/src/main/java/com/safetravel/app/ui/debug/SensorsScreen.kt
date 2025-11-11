package com.safetravel.app.ui.debug

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SensorsScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Get all available sensors
    val allSensors = remember {
        sensorManager.getSensorList(Sensor.TYPE_ALL)
    }

    // State for sensor data
    var sensorDataMap by remember { mutableStateOf<Map<Int, SensorData>>(emptyMap()) }

    // Register listeners for all sensors
    DisposableEffect(Unit) {
        val listeners = mutableMapOf<Int, SensorEventListener>()

        allSensors.forEach { sensor ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        sensorDataMap = sensorDataMap + (sensor.type to SensorData(
                            name = sensor.name,
                            type = getSensorTypeName(sensor.type),
                            vendor = sensor.vendor,
                            version = sensor.version,
                            power = sensor.power,
                            maxRange = sensor.maximumRange,
                            resolution = sensor.resolution,
                            values = it.values.toList(),
                            accuracy = it.accuracy,
                            timestamp = it.timestamp
                        ))
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Handle accuracy changes if needed
                }
            }

            listeners[sensor.type] = listener
            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            listeners.forEach { (_, listener) ->
                sensorManager.unregisterListener(listener)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Available Sensors: ${allSensors.size}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sensor list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allSensors) { sensor ->
                val sensorData = sensorDataMap[sensor.type]
                SensorCard(
                    sensor = sensor,
                    sensorData = sensorData
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun SensorCard(sensor: Sensor, sensorData: SensorData?) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sensor name and type
            Text(
                text = sensor.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = getSensorTypeName(sensor.type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                // Basic info
                SensorInfoRow("Vendor", sensor.vendor)
                SensorInfoRow("Version", sensor.version.toString())
                SensorInfoRow("Power", "${sensor.power} mA")
                SensorInfoRow("Max Range", "${sensor.maximumRange} ${getUnit(sensor.type)}")
                SensorInfoRow("Resolution", "${sensor.resolution} ${getUnit(sensor.type)}")

                // Current values
                if (sensorData != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                    Text(
                        text = "Current Values:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    sensorData.values.forEachIndexed { index, value ->
                        Text(
                            text = "Value [$index]: ${String.format("%.4f", value)} ${getUnit(sensor.type)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    SensorInfoRow("Accuracy", getAccuracyName(sensorData.accuracy))
                    SensorInfoRow("Timestamp", "${sensorData.timestamp / 1_000_000}ms")
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No data available yet...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Show current values in compact form
                if (sensorData != null && sensorData.values.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sensorData.values.joinToString(", ") {
                            String.format("%.2f", it)
                        } + " ${getUnit(sensor.type)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SensorInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

data class SensorData(
    val name: String,
    val type: String,
    val vendor: String,
    val version: Int,
    val power: Float,
    val maxRange: Float,
    val resolution: Float,
    val values: List<Float>,
    val accuracy: Int,
    val timestamp: Long
)

fun getSensorTypeName(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
        Sensor.TYPE_ORIENTATION -> "Orientation (Deprecated)"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        Sensor.TYPE_LIGHT -> "Light"
        Sensor.TYPE_PRESSURE -> "Pressure"
        Sensor.TYPE_TEMPERATURE -> "Temperature (Deprecated)"
        Sensor.TYPE_PROXIMITY -> "Proximity"
        Sensor.TYPE_GRAVITY -> "Gravity"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetic Field (Uncalibrated)"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector"
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyroscope (Uncalibrated)"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
        Sensor.TYPE_STEP_COUNTER -> "Step Counter"
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
        Sensor.TYPE_HEART_RATE -> "Heart Rate"
        Sensor.TYPE_POSE_6DOF -> "Pose 6DOF"
        Sensor.TYPE_STATIONARY_DETECT -> "Stationary Detect"
        Sensor.TYPE_MOTION_DETECT -> "Motion Detect"
        Sensor.TYPE_HEART_BEAT -> "Heart Beat"
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "Low Latency Off-body Detect"
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "Accelerometer (Uncalibrated)"
        else -> "Unknown Type ($type)"
    }
}

fun getUnit(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "m/s²"
        Sensor.TYPE_MAGNETIC_FIELD -> "µT"
        Sensor.TYPE_GYROSCOPE -> "rad/s"
        Sensor.TYPE_LIGHT -> "lx"
        Sensor.TYPE_PRESSURE -> "hPa"
        Sensor.TYPE_TEMPERATURE -> "°C" // Added this for the deprecated type
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
        Sensor.TYPE_PROXIMITY -> "cm"
        Sensor.TYPE_GRAVITY -> "m/s²"
        Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
        Sensor.TYPE_STEP_COUNTER -> "steps"
        Sensor.TYPE_HEART_RATE -> "bpm"
        else -> ""
    }
}

fun getAccuracyName(accuracy: Int): String {
    return when (accuracy) {
        SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
        else -> "Unknown"
    }
}