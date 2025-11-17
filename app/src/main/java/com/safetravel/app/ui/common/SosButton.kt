package com.safetravel.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SosButton(
    modifier: Modifier = Modifier,
    viewModel: SosButtonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = uiState.sosState) {
            is SosState.Idle, is SosState.Holding -> {
                SosActivationButton(
                    sosState = state,
                    onPress = viewModel::onButtonPress,
                    onRelease = viewModel::onButtonRelease
                )
            }
            is SosState.Countdown -> {
                SosCountdownDialog(state.secondsRemaining, viewModel::onImOkayClick, viewModel::onSendHelpClick)
            }
            is SosState.PasscodeEntry -> {
                SosPasscodeDialog(
                    secondsRemaining = state.secondsRemaining,
                    error = uiState.passcodeError,
                    onVerify = viewModel::onVerifyPasscode,
                    onDismiss = viewModel::onPasscodeDialogDismiss // Use the new dismiss handler
                )
            }
            is SosState.NavigateToAiHelp -> {
                // This state triggers navigation in the parent. Show a placeholder while navigating.
                Spacer(modifier = Modifier.size(100.dp))
            }
        }
    }
}

@Composable
private fun SosActivationButton(
    sosState: SosState,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onPress()
        } else {
            onRelease()
        }
    }

    val holdProgress by animateFloatAsState(
        targetValue = if (sosState is SosState.Holding) 1f else 0f,
        animationSpec = tween(durationMillis = 5000),
        label = "HoldProgress"
    )

    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = holdProgress,
            modifier = Modifier.size(120.dp),
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )

        Button(
            onClick = { /* Clicks are disabled, only press and hold works */ },
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(100.dp)
        ) {
            Text("SOS", fontSize = 24.sp)
        }
    }
}

@Composable
private fun SosCountdownDialog(secondsRemaining: Int, onImOkayClick: () -> Unit, onSendHelpClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Are you okay?", textAlign = TextAlign.Center) },
        text = {
            Text(
                text = "Sending alert in $secondsRemaining seconds",
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
private fun SosPasscodeDialog(secondsRemaining: Int, error: String?, onVerify: (String) -> Unit, onDismiss: () -> Unit) {
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
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onVerify(passcode) }) {
                Text("Confirm")
            }
        }
    )
}
