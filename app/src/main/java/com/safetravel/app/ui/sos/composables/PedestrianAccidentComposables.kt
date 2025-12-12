package com.safetravel.app.ui.sos.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.app.ui.sos.data.DetectionState
import com.safetravel.app.ui.sos.data.DetectionStateEnum
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

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
                text = if (accidentDetected) "ACCIDENT DETECTED" else "MONITORING ACTIVE",
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
                    text = "Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(detectionTime)}",
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
                text = "System 1 (Standard)",
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

            // Context
            SectionHeader("Context")
            MetricRow("Speed", String.format("%.2f m/s", state.speedMps))
            MetricRow("Speed delta", String.format("%.2f m/s", state.speedDeltaMps))
            MetricRow("Activity Hint", state.activityHint.name)

            Divider(Modifier.padding(vertical = 12.dp))

            // Raw Accelerometer
            SectionHeader("Raw Accelerometer")
            MetricRow("X-axis", String.format("%.3f m/s^2", state.accelX))
            MetricRow("Y-axis", String.format("%.3f m/s^2", state.accelY))
            MetricRow("Z-axis", String.format("%.3f m/s^2", state.accelZ))

            Divider(Modifier.padding(vertical = 12.dp))

            // TAM (Total Acceleration Magnitude)
            SectionHeader("Total Acceleration Magnitude (TAM)")
            MetricRow("Formula", "sqrt(ax^2 + ay^2 + az^2)")
            MetricRow("Value", String.format("%.3f m/s^2", state.tam), highlight = state.tam > 20f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Jerk
            SectionHeader("Jerk (Rate of Change)")
            MetricRow("Formula", "sqrt(jx^2 + jy^2 + jz^2)")
            MetricRow("Value", String.format("%.2f m/s^3", state.jerk), highlight = state.jerk > 100f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Angular Velocity
            SectionHeader("Angular Velocity")
            MetricRow("wx", String.format("%.3f rad/s", state.gyroX))
            MetricRow("wy", String.format("%.3f rad/s", state.gyroY))
            MetricRow("wz", String.format("%.3f rad/s", state.gyroZ))
            MetricRow("Magnitude", String.format("%.3f rad/s", state.angularMagnitude), highlight = state.angularMagnitude > 4f)

            Divider(Modifier.padding(vertical = 12.dp))

            // Gravity & Orientation
            SectionHeader("Gravity Vector & Orientation")
            MetricRow("Gravity X", String.format("%.3f m/s^2", state.gravityX))
            MetricRow("Gravity Y", String.format("%.3f m/s^2", state.gravityY))
            MetricRow("Gravity Z", String.format("%.3f m/s^2", state.gravityZ))
            MetricRow("Orientation Change", String.format("%.1f deg", state.orientationChange), highlight = state.orientationChange > 60f)

            Divider(Modifier.padding(vertical = 12.dp))

            // SMA (Signal Magnitude Area)
            SectionHeader("Signal Magnitude Area (1s window)")
            MetricRow("SMA", String.format("%.3f m/s^2", state.sma))
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

            ThresholdRow("TAM > 20 m/s^2", state.tam > 20f)
            ThresholdRow("Jerk > 100 m/s^3", state.jerk > 100f)
            ThresholdRow("Angular > 4 rad/s", state.angularMagnitude > 4f)
            ThresholdRow("Orientation > 60 deg", state.orientationChange > 60f)
            ThresholdRow("Duration > 150ms", state.impactDuration > 150)
            ThresholdRow("Speed drop > 8 m/s", state.speedDeltaMps > 8f)

            Spacer(modifier = Modifier.height(12.dp))

            val conditionsMet = listOf(
                state.tam > 20f,
                state.jerk > 100f,
                state.angularMagnitude > 4f || state.orientationChange > 60f,
                state.impactDuration > 150,
                state.speedDeltaMps > 8f
            ).count { it }

            Text(
                text = "Conditions Met: $conditionsMet / 5",
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
                text = "Monitoring for stillness and speed drop...",
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

            MetricRow("Current SMA", String.format("%.3f m/s^2", state.sma))
            MetricRow("Stillness Required", "< 1.0 m/s^2")
            MetricRow("Speed", String.format("%.2f m/s", state.speedMps))
            MetricRow("Speed drop", if (state.speedDeltaMps > 8f) "YES" else "NO")
            MetricRow("User Stationary", if (state.sma < 1.0f) "YES" else "NO")
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
                text = if (met) "Y" else "N",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun DebugDequeCard(deque: ArrayDeque<DetectionState>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Debug Deque",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Deque size: ${deque.size}")

            Spacer(modifier = Modifier.height(8.dp))

            deque.takeLast(5).forEachIndexed { index, state ->
                Text("  [$index] TAM: ${String.format("%.3f", state.tam)}, v=${String.format("%.2f", state.speedMps)} m/s")
            }
        }
    }
}

@Composable
fun PaperDetectorStatusCard(stateName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (stateName) {
                "IDLE" -> MaterialTheme.colorScheme.surfaceVariant
                "FREE_FALL_DETECTED" -> MaterialTheme.colorScheme.tertiaryContainer
                "IMPACT_DETECTED" -> MaterialTheme.colorScheme.secondaryContainer
                "CONFIRMED" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System 2 (Paper Algorithm)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stateName.replace("_", " "),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VolumeSosStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Volume Button SOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Press volume buttons 10 times rapidly to trigger SOS.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AccidentCountdownDialog(
    secondsRemaining: Int,
    onImOkayClick: () -> Unit,
    onSendHelpClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Are you okay?", textAlign = TextAlign.Center) },
        text = {
            Text(
                text = "Accident detected. Sending alert in $secondsRemaining seconds",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onSendHelpClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SEND HELP NOW")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onImOkayClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I'm Okay")
            }
        }
    )
}

@Composable
fun AccidentPasscodeDialog(
    secondsRemaining: Int,
    error: String?,
    onVerify: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Passcode to Cancel") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sending alert in $secondsRemaining seconds",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(passcode) }
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
