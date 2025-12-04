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
import com.safetravel.app.ui.sos.composables.AccidentCountdownDialog
import com.safetravel.app.ui.sos.composables.AccidentPasscodeDialog
import com.safetravel.app.ui.sos.composables.AlertStatusCard
import com.safetravel.app.ui.sos.composables.CalculationsCard
import com.safetravel.app.ui.sos.composables.DebugDequeCard
import com.safetravel.app.ui.sos.composables.DetectionStateCard
import com.safetravel.app.ui.sos.composables.PaperDetectorStatusCard
import com.safetravel.app.ui.sos.composables.PostImpactCard
import com.safetravel.app.ui.sos.composables.ThresholdStatusCard
import com.safetravel.app.ui.sos.composables.VolumeSosStatusCard
import com.safetravel.app.ui.sos.data.DetectionStateEnum

@Composable
fun AccidentDetectionScreen(
    viewModel: PedestrianAccidentScreenViewModel = hiltViewModel()
) {
    val detectionState by viewModel.detectionState.collectAsState()
    val paperStateName by viewModel.paperStateName.collectAsState()
    val accidentDetected by viewModel.accidentDetected.collectAsState()
    val detectionTime by viewModel.detectionTime.collectAsState()
    val sensorDataDeque by viewModel.sensorDataDeque.collectAsState()
    
    // Countdown State
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val showPasscodeDialog by viewModel.showPasscodeDialog.collectAsState()
    val passcodeError by viewModel.passcodeError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Alert Status Card (Combined Alert)
        AlertStatusCard(
            accidentDetected = accidentDetected,
            detectionTime = detectionTime,
            onReset = { viewModel.onReset() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System 1 Status
        DetectionStateCard(detectionState.currentState)

        Spacer(modifier = Modifier.height(8.dp))

        // System 2 Status
        PaperDetectorStatusCard(paperStateName)

        Spacer(modifier = Modifier.height(8.dp))

        // Volume SOS Status (New)
        VolumeSosStatusCard()

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time Calculations (Based on System 1's processed data)
        CalculationsCard(detectionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Threshold Status (System 1)
        ThresholdStatusCard(detectionState)

        Spacer(modifier = Modifier.height(16.dp))

        // Post-Impact Monitoring (System 1)
        if (detectionState.currentState == DetectionStateEnum.VALIDATING) {
            PostImpactCard(detectionState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug Deque Card
        DebugDequeCard(sensorDataDeque)
    }

    // Countdown Dialog
    if (isCountdownActive && !showPasscodeDialog) {
        AccidentCountdownDialog(
            secondsRemaining = countdownSeconds,
            onImOkayClick = viewModel::onImOkayClick,
            onSendHelpClick = viewModel::onSendHelpClick
        )
    }

    // Passcode Dialog
    if (showPasscodeDialog) {
        AccidentPasscodeDialog(
            secondsRemaining = countdownSeconds,
            error = passcodeError,
            onVerify = viewModel::onVerifyPasscode,
            onDismiss = viewModel::onPasscodeDialogDismiss
        )
    }
}
