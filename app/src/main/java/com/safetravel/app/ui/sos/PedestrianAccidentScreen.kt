package com.safetravel.app.ui.sos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.ui.sos.composables.AlertStatusCard
import com.safetravel.app.ui.sos.composables.CalculationsCard
import com.safetravel.app.ui.sos.composables.DebugDequeCard
import com.safetravel.app.ui.sos.composables.DetectionStateCard
import com.safetravel.app.ui.sos.composables.PostImpactCard
import com.safetravel.app.ui.sos.composables.ThresholdStatusCard
import com.safetravel.app.ui.sos.data.DetectionStateEnum

@Composable
fun AccidentDetectionScreen(
    viewModel: PedestrianAccidentScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    viewModel.init(context)

    val detectionState by viewModel.detectionState.collectAsState()
    val accidentDetected by viewModel.accidentDetected.collectAsState()
    val detectionTime by viewModel.detectionTime.collectAsState()
    val sensorDataDeque by viewModel.sensorDataDeque.collectAsState()

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
            onReset = { viewModel.onReset() }
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

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Deque Card
        DebugDequeCard(sensorDataDeque)
    }
}
