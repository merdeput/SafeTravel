package com.safetravel.app.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.BluetoothBleManager
import com.safetravel.app.data.repository.BluetoothScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BluetoothHearingUiState(
    val isScanning: Boolean = false,
    val detectedSignals: Map<String, com.safetravel.app.data.repository.DetectedSosSignal> = emptyMap()
)

@HiltViewModel
class BluetoothHearingViewModel @Inject constructor(
    private val scanRepository: BluetoothScanRepository,
    private val bleManager: BluetoothBleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothHearingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scanRepository.isScanning.collectLatest { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }

        viewModelScope.launch {
            scanRepository.detectedSignals.collectLatest { signals ->
                _uiState.update { it.copy(detectedSignals = signals) }
            }
        }
    }

    fun toggleScanning() {
        if (uiState.value.isScanning) {
            bleManager.stopScanning()
        } else {
            bleManager.startScanning()
        }
    }
}
