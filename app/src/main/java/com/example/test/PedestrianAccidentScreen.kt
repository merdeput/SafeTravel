package com.example.test

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun AccidentDetectionScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) }
    val gyroscope = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val gravity = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) }

    var detectionState by remember { mutableStateOf(DetectionState()) }
    var accidentDetected by remember { mutableStateOf(false) }
    var detectionTime by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val detector = AccidentDetector()

        val accelListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val result = detector.processAccelerometer(it.values, it.timestamp)
                    detectionState = result

                    if (result.accidentConfirmed && !accidentDetected) {
                        accidentDetected = true
                        detectionTime = System.currentTimeMillis()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val gyroListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    detector.processGyroscope(it.values)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val gravityListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    detector.processGravity(it.values)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gravity?.let {
            sensorManager.registerListener(gravityListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        onDispose {
            sensorManager.unregisterListener(accelListener)
            sensorManager.unregisterListener(gyroListener)
            sensorManager.unregisterListener(gravityListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Alert Status Card
        AlertStatusCard(
            accidentDetected = accidentDetected,
            detectionTime = detectionTime,
            onReset = {
                accidentDetected = false
                detectionTime = 0L
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Detection State Card
        DetectionStateCard(detectionState.currentState)

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time Calculations
        CalculationsCard(detectionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Threshold Status
        ThresholdStatusCard(detectionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Post-Impact Monitoring
        if (detectionState.currentState == DetectionStateEnum.VALIDATING) {
            PostImpactCard(detectionState)
        }
    }
}

@Composable
fun AlertStatusCard(
    accidentDetected: Boolean,
    detectionTime: Long,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (accidentDetected)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (accidentDetected) "⚠️ ACCIDENT DETECTED" else "✓ MONITORING ACTIVE",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (accidentDetected)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            if (accidentDetected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(detectionTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("RESET ALERT")
                }
            }
        }
    }
}

@Composable
fun DetectionStateCard(state: DetectionStateEnum) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                DetectionStateEnum.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                DetectionStateEnum.MONITORING -> MaterialTheme.colorScheme.primaryContainer
                DetectionStateEnum.POTENTIAL_IMPACT -> MaterialTheme.colorScheme.tertiaryContainer
                DetectionStateEnum.VALIDATING -> MaterialTheme.colorScheme.secondaryContainer
                DetectionStateEnum.CONFIRMED_ACCIDENT -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detection State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.name.replace("_", " "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun CalculationsCard(state: DetectionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Real-time Calculations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Raw Accelerometer
            SectionHeader("Raw Accelerometer")
            MetricRow("X-axis", String.format("%.3f m/s²", state.accelX))
            MetricRow("Y-axis", String.format("%.3f m/s²", state.accelY))
            MetricRow("Z-axis", String.format("%.3f m/s²", state.accelZ))

            Divider(Modifier.padding(vertical = 12.dp))

            // TAM (Total Acceleration Magnitude)
            SectionHeader("Total Acceleration Magnitude (TAM)")
            MetricRow("Formula", "√(ax² + ay² + az²)")
            MetricRow("Value", String.format("%.3f m/s²", state.tam), highlight = state.tam > 20f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Jerk
            SectionHeader("Jerk (Rate of Change)")
            MetricRow("Formula", "√(jx² + jy² + jz²)")
            MetricRow("Value", String.format("%.2f m/s³", state.jerk), highlight = state.jerk > 100f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Angular Velocity
            SectionHeader("Angular Velocity")
            MetricRow("ωx", String.format("%.3f rad/s", state.gyroX))
            MetricRow("ωy", String.format("%.3f rad/s", state.gyroY))
            MetricRow("ωz", String.format("%.3f rad/s", state.gyroZ))
            MetricRow("Magnitude", String.format("%.3f rad/s", state.angularMagnitude), highlight = state.angularMagnitude > 4f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Gravity & Orientation
            SectionHeader("Gravity Vector & Orientation")
            MetricRow("Gravity X", String.format("%.3f m/s²", state.gravityX))
            MetricRow("Gravity Y", String.format("%.3f m/s²", state.gravityY))
            MetricRow("Gravity Z", String.format("%.3f m/s²", state.gravityZ))
            MetricRow("Orientation Change", String.format("%.1f°", state.orientationChange), highlight = state.orientationChange > 60f)

            Divider(Modifier.padding(vertical = 12.dp))

            // SMA (Signal Magnitude Area)
            SectionHeader("Signal Magnitude Area (1s window)")
            MetricRow("SMA", String.format("%.3f m/s²", state.sma))
        }
    }
}

@Composable
fun ThresholdStatusCard(state: DetectionState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Threshold Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            ThresholdRow("TAM > 20 m/s²", state.tam > 20f)
            ThresholdRow("Jerk > 100 m/s³", state.jerk > 100f)
            ThresholdRow("Angular > 4 rad/s", state.angularMagnitude > 4f)
            ThresholdRow("Orientation > 60°", state.orientationChange > 60f)
            ThresholdRow("Duration > 150ms", state.impactDuration > 150)

            Spacer(modifier = Modifier.height(12.dp))

            val conditionsMet = listOf(
                state.tam > 20f,
                state.jerk > 100f,
                state.angularMagnitude > 4f || state.orientationChange > 60f,
                state.impactDuration > 150
            ).count { it }

            Text(
                text = "Conditions Met: $conditionsMet / 4",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (conditionsMet >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PostImpactCard(state: DetectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Post-Impact Validation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val elapsed = (System.currentTimeMillis() - state.validationStartTime) / 1000f
            val remaining = max(0f, 30f - elapsed)

            Text(
                text = "Monitoring for stillness...",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (elapsed / 30f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = String.format("Time remaining: %.1fs", remaining),
                style = MaterialTheme.typography.bodySmall
            )

            MetricRow("Current SMA", String.format("%.3f m/s²", state.sma))
            MetricRow("Stillness Required", "< 1.0 m/s²")
            MetricRow("User Stationary", if (state.sma < 1.0f) "YES ✓" else "NO ✗")
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun MetricRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ThresholdRow(condition: String, met: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = condition,
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (met) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (met) "✓" else "✗",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Data classes
data class DetectionState(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val tam: Float = 0f,
    val jerk: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val angularMagnitude: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val orientationChange: Float = 0f,
    val sma: Float = 0f,
    val impactDuration: Long = 0L,
    val currentState: DetectionStateEnum = DetectionStateEnum.IDLE,
    val accidentConfirmed: Boolean = false,
    val validationStartTime: Long = 0L
)

enum class DetectionStateEnum {
    IDLE,
    MONITORING,
    POTENTIAL_IMPACT,
    VALIDATING,
    CONFIRMED_ACCIDENT
}

// Accident Detection Logic
class AccidentDetector {
    private var previousAccel = FloatArray(3)
    private var previousTimestamp = 0L

    // Stores the gravity vector from *before* the impact
    private var gravityAtRest = FloatArray(3) { 0f }
    private var isFirstGravityReading = true

    private val accelBuffer = mutableListOf<Float>()
    private val maxBufferSize = 50 // 1 second at 50Hz (approx 20ms per reading)

    private var currentState = DetectionStateEnum.IDLE
    private var impactStartTime = 0L
    private var validationStartTime = 0L

    private var currentGyro = FloatArray(3)
    private var currentGravity = FloatArray(3)

    fun processAccelerometer(values: FloatArray, timestamp: Long): DetectionState {
        // --- 1. Calculations ---

        val tam = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))

        val deltaTime = if (previousTimestamp > 0) {
            (timestamp - previousTimestamp) / 1_000_000_000.0 // Convert to seconds
        } else {
            0.02 // Default 50Hz
        }

        val jerk = if (previousTimestamp > 0 && deltaTime > 0) {
            val jx = (values[0] - previousAccel[0]) / deltaTime.toFloat()
            val jy = (values[1] - previousAccel[1]) / deltaTime.toFloat()
            val jz = (values[2] - previousAccel[2]) / deltaTime.toFloat()
            sqrt(jx.pow(2) + jy.pow(2) + jz.pow(2))
        } else {
            0f
        }

        accelBuffer.add(abs(values[0]) + abs(values[1]) + abs(values[2]))
        if (accelBuffer.size > maxBufferSize) {
            accelBuffer.removeAt(0)
        }
        val sma = if (accelBuffer.isNotEmpty()) accelBuffer.average().toFloat() else 0f

        val orientationChange = calculateOrientationChange(currentGravity, gravityAtRest)
        val angularMagnitude = sqrt(currentGyro[0].pow(2) + currentGyro[1].pow(2) + currentGyro[2].pow(2))

        // --- 2. State Machine Logic ---
        val now = System.currentTimeMillis()

        when (currentState) {
            DetectionStateEnum.IDLE -> {
                if (!isFirstGravityReading) {
                    currentState = DetectionStateEnum.MONITORING
                }
            }
            DetectionStateEnum.MONITORING -> {
                // REVISED: Trigger on SPIKE only.
                if (tam > 20f && jerk > 100f) {
                    // This is the impact. Store the "before" state.
                    gravityAtRest = currentGravity.copyOf()
                    currentState = DetectionStateEnum.POTENTIAL_IMPACT
                    impactStartTime = now
                }
            }
            DetectionStateEnum.POTENTIAL_IMPACT -> {
                val duration = now - impactStartTime

                // Wait 2 seconds for the event to finish
                if (duration > 2000) {
                    // REVISED: Check aftermath.
                    // Condition A: Did orientation change? (A fall/rollover)
                    val orientationChanged = orientationChange > 60f
                    // Condition B: Is the user motionless *right now*? (A crash/unconscious)
                    val isStillNow = sma < 1.0f

                    if (orientationChanged || isStillNow) {
                        // High confidence. Start final 30-second stillness check.
                        currentState = DetectionStateEnum.VALIDATING
                        validationStartTime = now
                    } else {
                        // False alarm. (e.g., dropped phone, picked it right up)
                        currentState = DetectionStateEnum.MONITORING
                    }
                    impactStartTime = 0L
                }
                // (Else: keep waiting for the 2-second window to pass)
            }
            DetectionStateEnum.VALIDATING -> {
                val elapsed = now - validationStartTime

                // Wait 30 seconds
                if (elapsed > 30000) {
                    // Final Check: Is the user *still* motionless?
                    if (sma < 1.0f) {
                        currentState = DetectionStateEnum.CONFIRMED_ACCIDENT
                    } else {
                        // User got up or is moving. False alarm.
                        currentState = DetectionStateEnum.MONITORING
                        validationStartTime = 0L
                    }
                }
            }
            DetectionStateEnum.CONFIRMED_ACCIDENT -> {
                // Stay in this state until reset from UI
            }
        }

        // --- 3. Update & Return ---
        previousAccel = values.copyOf()
        previousTimestamp = timestamp

        val impactDuration = if (impactStartTime > 0) now - impactStartTime else 0L

        return DetectionState(
            accelX = values[0],
            accelY = values[1],
            accelZ = values[2],
            tam = tam,
            jerk = jerk,
            gyroX = currentGyro[0],
            gyroY = currentGyro[1],
            gyroZ = currentGyro[2],
            angularMagnitude = angularMagnitude,
            gravityX = currentGravity[0],
            gravityY = currentGravity[1],
            gravityZ = currentGravity[2],
            orientationChange = orientationChange,
            sma = sma,
            impactDuration = impactDuration,
            currentState = currentState,
            accidentConfirmed = currentState == DetectionStateEnum.CONFIRMED_ACCIDENT,
            validationStartTime = validationStartTime
        )
    }

    fun processGyroscope(values: FloatArray) {
        currentGyro = values.copyOf()
    }

    fun processGravity(values: FloatArray) {
        currentGravity = values.copyOf()

        // REVISED: Only update "at rest" gravity if we are in IDLE
        // or if the user is not moving much (low SMA).
        // This prevents updating 'gravityAtRest' with a bad value
        // during normal, non-impact-related motion.
        val sma = if (accelBuffer.isNotEmpty()) accelBuffer.average().toFloat() else 0f

        if (isFirstGravityReading && values.any { it != 0f }) {
            gravityAtRest = values.copyOf()
            isFirstGravityReading = false
        } else if (currentState == DetectionStateEnum.MONITORING && sma < 1.5f) {
            // Continuously update "at rest" gravity while user is relatively still
            gravityAtRest = values.copyOf()
        }
    }

    private fun calculateOrientationChange(current: FloatArray, previous: FloatArray): Float {
        // Check if either vector is uninitialized
        if (previous.all { it == 0f } || current.all { it == 0f }) return 0f

        val dotProduct = current[0] * previous[0] + current[1] * previous[1] + current[2] * previous[2]
        val mag1 = sqrt(current[0].pow(2) + current[1].pow(2) + current[2].pow(2))
        val mag2 = sqrt(previous[0].pow(2) + previous[1].pow(2) + previous[2].pow(2))

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble()).toFloat().toDouble()).toFloat()
    }
}