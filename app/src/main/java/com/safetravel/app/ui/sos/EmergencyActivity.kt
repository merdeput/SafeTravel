package com.safetravel.app.ui.sos

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.MainActivity
import com.safetravel.app.ui.theme.BeeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make this activity appear over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        enableEdgeToEdge()

        setContent {
            BeeTheme {
                // Using Hilt to inject ViewModel into the composable tree
                val viewModel: AiHelpViewModel = hiltViewModel()
                EmergencyCardScreen(
                    settingsRepository = settingsRepository,
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun EmergencyCardScreen(
    settingsRepository: SettingsRepository,
    viewModel: AiHelpViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Collecting settings from DataStore
    val settings by settingsRepository.settingsFlow.collectAsState(initial = null)
    val info = settings?.emergencyInfo
    val uiState by viewModel.uiState.collectAsState()
    
    var showPasscodeDialog by remember { mutableStateOf(false) }

    // Close screen when emergency is stopped
    LaunchedEffect(uiState.emergencyStopped) {
        if (uiState.emergencyStopped) {
            onDismiss()
        }
    }

    if (showPasscodeDialog) {
        SosPasscodeDialog(
            error = uiState.passcodeError,
            onVerify = { inputPasscode ->
                viewModel.onVerifyPasscode(inputPasscode)
            },
            onDismiss = { 
                showPasscodeDialog = false
                // Optional: clear error in VM if needed, though VM logic handles resets on success
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "EMERGENCY INFO",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (info != null) {
                    EmergencyInfoItem(label = "Name", value = info.name)
                    EmergencyInfoItem(label = "Blood Type", value = info.bloodType)
                    EmergencyInfoItem(label = "Allergies", value = info.allergies)
                    EmergencyInfoItem(label = "Conditions", value = info.conditions)
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text(
                        text = "Emergency Contact",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    EmergencyInfoItem(label = "Name", value = info.contactName)
                    EmergencyInfoItem(label = "Phone", value = info.contactPhone)
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(48.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("navigation_route", "ai_help")
                            }
                            context.startActivity(intent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("AI HELP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("DISMISS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bluetooth Beacon Button at the top right
            FilledTonalIconButton(
                onClick = { viewModel.toggleBleAdvertising() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 24.dp), // Adjust for system bars if needed
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (uiState.isAdvertisingBle) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.isAdvertisingBle) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                 val icon = if (uiState.isAdvertisingBle) Icons.Default.BluetoothSearching else Icons.Default.BluetoothDisabled
                 Icon(icon, contentDescription = "Toggle Bluetooth Beacon")
            }
        }
    }
}

@Composable
fun EmergencyInfoItem(label: String, value: String) {
    if (value.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SosPasscodeDialog(error: String?, onVerify: (String) -> Unit, onDismiss: () -> Unit) {
    var passcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop SOS") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Enter passcode to stop emergency mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
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
        },
        dismissButton = {
             TextButton(onClick = onDismiss) {
                 Text("Cancel")
             }
        }
    )
}
