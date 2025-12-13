package com.safetravel.app.ui.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHelpScreen(
    viewModel: AiHelpViewModel = hiltViewModel(),
    onEmergencyStopped: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showStopDialog by remember { mutableStateOf(false) }

    // Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    // Speech Recognition Manager
    val speechRecognizer = remember { SpeechRecognizerManager(context) }

    // Audio Recording Permission handling (without Accompanist)
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            // Start recording immediately after permission granted
            speechRecognizer.startListening { transcription ->
                viewModel.onTranscriptionReceived(transcription)
            }
            viewModel.setRecordingState(true)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    // Initialize TTS
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsInitialized = true
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer.destroy()
        }
    }

    // Auto-speak AI responses
    LaunchedEffect(uiState.messages.size) {
        if (uiState.voiceEnabled && ttsInitialized && uiState.messages.isNotEmpty()) {
            val lastMessage = uiState.messages.last()
            if (!lastMessage.isFromUser) {
                viewModel.setSpeakingState(true)
                tts?.speak(lastMessage.message, TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                // Wait for speech to complete (approximate)
                kotlinx.coroutines.delay(lastMessage.message.length * 50L)
                viewModel.setSpeakingState(false)
            }
        }
    }

    LaunchedEffect(uiState.emergencyStopped) {
        if (uiState.emergencyStopped) {
            onEmergencyStopped()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Emergency Assistant") },
                actions = {
                    // Stop speaking button
                    IconButton(onClick = {
                        tts?.stop()
                        viewModel.setSpeakingState(false)
                    }) {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = "Stop Speaking",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Quick Actions section
            EmergencyUtilsSection()

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Voice/Speaking indicators
            if (uiState.isRecording) {
                VoiceRecordingIndicator()
            }

            if (uiState.isSpeaking) {
                AISpeakingIndicator()
            }

            // Chat messages take up the remaining space
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(uiState.messages.reversed()) { chatMessage ->
                    MessageBubble(chatMessage)
                }
            }

            // Controls at the bottom
            Column(modifier = Modifier.padding(16.dp)) {
                // Stop Emergency Button
                Button(
                    onClick = { showStopDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Emergency")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Voice + Text Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Voice Input Button
                    FloatingActionButton(
                        onClick = {
                            if (hasAudioPermission) {
                                if (uiState.isRecording) {
                                    // Stop recording
                                    speechRecognizer.stopListening()
                                    viewModel.setRecordingState(false)
                                } else {
                                    // Start recording
                                    speechRecognizer.startListening { transcription ->
                                        viewModel.onTranscriptionReceived(transcription)
                                    }
                                    viewModel.setRecordingState(true)
                                }
                            } else {
                                // Request permission
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        containerColor = if (uiState.isRecording)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (uiState.isRecording) "Stop recording" else "Start voice input"
                        )
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = uiState.currentQuery,
                        onValueChange = viewModel::onQueryChange,
                        label = { Text("Ask for help...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        trailingIcon = {
                            IconButton(
                                onClick = viewModel::onAskClick,
                                enabled = !uiState.isAwaitingResponse && uiState.currentQuery.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send message"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showStopDialog) {
        StopEmergencyDialog(
            passcodeError = uiState.passcodeError,
            onConfirm = viewModel::onVerifyPasscode,
            onDismiss = { showStopDialog = false }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isFromUser)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun VoiceRecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.error, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Listening...",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AISpeakingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "AI is speaking...",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmergencyUtilsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmergencyCallButton(
                label = "Police",
                phoneNumber = "113",
                icon = Icons.Default.LocalPolice,
                modifier = Modifier.weight(1f)
            )
            EmergencyCallButton(
                label = "Ambulance",
                phoneNumber = "115",
                icon = Icons.Default.MedicalServices,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyCallButton(
    label: String,
    phoneNumber: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialIntent = remember {
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    }

    Card(
        onClick = {
            try {
                context.startActivity(dialIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StopEmergencyDialog(
    passcodeError: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop Emergency") },
        text = {
            Column {
                Text("Enter your passcode to confirm you are safe and stop the emergency alert.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
                    isError = passcodeError != null
                )
                passcodeError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(passcode) }) {
                Text("Confirm & Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}