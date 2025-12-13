package com.safetravel.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SosButton(
    modifier: Modifier = Modifier,
    viewModel: SosButtonViewModel // Hoisted from MainAppScreen
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
                    onDismiss = viewModel::onPasscodeDialogDismiss 
                )
            }
            is SosState.NavigateToAiHelp -> {
                // The button becomes effectively invisible while in this state
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
        animationSpec = tween(durationMillis = 4000),
        label = "HoldProgress"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "ButtonScale"
    )

    Box(contentAlignment = Alignment.Center) {
        // Progress Indicator Ring
        CircularProgressIndicator(
            progress = { holdProgress },
            modifier = Modifier.size(140.dp),
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        )

        // The Main Button
        Surface(
            modifier = Modifier
                .size(110.dp)
                .scale(scale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 6.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // handled by custom scale animation
                    onClick = { } // No-op click, logic is in press/release
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "SOS", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SosCountdownDialog(secondsRemaining: Int, onImOkayClick: () -> Unit, onSendHelpClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { 
            Text(
                "Are you okay?", 
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sending alert in",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$secondsRemaining",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "seconds",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSendHelpClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SEND HELP NOW", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onImOkayClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
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
        title = { Text("Cancel SOS") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sending alert in $secondsRemaining s",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Enter Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(passcode) },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Confirm")
            }
        }
    )
}
