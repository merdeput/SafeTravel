package com.safetravel.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedSosSignal(
    val address: String,
    val rssi: Int,
    val lastSeenTimestamp: Long,
    val distanceEstimate: Double // Rough estimate
)

@Singleton
class BluetoothScanRepository @Inject constructor() {
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _detectedSignals = MutableStateFlow<Map<String, DetectedSosSignal>>(emptyMap())
    val detectedSignals = _detectedSignals.asStateFlow()

    fun setScanningState(isScanning: Boolean) {
        _isScanning.value = isScanning
    }

    fun addOrUpdateSignal(address: String, rssi: Int) {
        // Rough distance calculation (Path Loss Model) - very approximate
        // Distance = 10 ^ ((TxPower - RSSI) / (10 * N))
        // Assuming TxPower ~ -59dBm at 1m, N ~ 2 (Free space) to 4 (Indoor)
        val txPower = -59
        val n = 2.0
        val distance = Math.pow(10.0, (txPower - rssi) / (10 * n))

        val signal = DetectedSosSignal(
            address = address,
            rssi = rssi,
            lastSeenTimestamp = System.currentTimeMillis(),
            distanceEstimate = distance
        )
        
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
