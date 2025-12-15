package com.safetravel.app.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedSosSignal(
    val address: String,
    val rssi: Int,
    val lastSeenTimestamp: Long,
    val distanceEstimate: Double, // Rough estimate
    val message: String? = null // Decoded message from BLE packet
)

@Singleton
class BluetoothScanRepository @Inject constructor() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _detectedSignals = MutableStateFlow<Map<String, DetectedSosSignal>>(emptyMap())
    val detectedSignals = _detectedSignals.asStateFlow()
    
    // Flow to emit event when a NEW significant signal is detected (for notifications)
    private val _newSignalDetected = MutableSharedFlow<DetectedSosSignal>(extraBufferCapacity = 1)
    val newSignalDetected: SharedFlow<DetectedSosSignal> = _newSignalDetected.asSharedFlow()

    fun setScanningState(isScanning: Boolean) {
        _isScanning.value = isScanning
        // Clear signals when scanning is stopped
        if (!isScanning) {
            clearSignals()
        }
    }

    fun addOrUpdateSignal(address: String, rssi: Int, message: String?) {
        // Rough distance calculation (Path Loss Model)
        // Distance = 10 ^ ((TxPower - RSSI) / (10 * N))
        val txPower = -59
        val n = 2.0
        val distance = Math.pow(10.0, (txPower - rssi) / (10 * n))

        // Preserve existing message if the new one is null (e.g. unstable scan result)
        val existing = _detectedSignals.value[address]
        val displayMessage = message ?: existing?.message

        val signal = DetectedSosSignal(
            address = address,
            rssi = rssi,
            lastSeenTimestamp = System.currentTimeMillis(),
            distanceEstimate = distance,
            message = displayMessage
        )
        
        // Notify if it's a new signal or re-appeared after a while (e.g., > 10s)
        val isNew = existing == null
        val isReappeared = existing != null && (System.currentTimeMillis() - existing.lastSeenTimestamp > 10000)
        
        if (isNew || isReappeared) {
            _newSignalDetected.tryEmit(signal)
        }
        
        _detectedSignals.update { currentMap ->
            currentMap.toMutableMap().apply {
                put(address, signal)
            }
        }
    }

    fun clearSignals() {
        _detectedSignals.value = emptyMap()
    }
}
