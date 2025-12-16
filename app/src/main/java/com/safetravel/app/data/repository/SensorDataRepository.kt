package com.safetravel.app.data.repository

import com.safetravel.app.ui.sos.data.DetectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_DEQUE_SIZE = 300

@Singleton
class SensorDataRepository @Inject constructor() {

    sealed class DetectorTrigger {
        object Accident : DetectorTrigger()
        object Fall : DetectorTrigger()
        object VolumeSos : DetectorTrigger()
    }

    private val _sensorDataDeque = MutableStateFlow<ArrayDeque<DetectionState>>(ArrayDeque())
    val sensorDataDeque = _sensorDataDeque.asStateFlow()

    private val _latestDetectionState = MutableStateFlow(DetectionState())
    val latestDetectionState = _latestDetectionState.asStateFlow()

    private val _paperStateName = MutableStateFlow("IDLE")
    val paperStateName = _paperStateName.asStateFlow()

    private val _detectorEvents = MutableSharedFlow<DetectorTrigger>(extraBufferCapacity = 1)
    val detectorEvents = _detectorEvents.asSharedFlow()

    // New Flow for Reset Events
    private val _resetEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetEvents = _resetEvents.asSharedFlow()

    fun addSensorData(state: DetectionState) {
        // Update latest state
        _latestDetectionState.value = state

        // Update deque
        val newDeque = ArrayDeque(_sensorDataDeque.value)
        if (newDeque.size >= MAX_DEQUE_SIZE) {
            newDeque.removeFirst()
        }
        newDeque.addLast(state)
        _sensorDataDeque.value = newDeque
    }

    fun updatePaperState(stateName: String) {
        _paperStateName.value = stateName
    }

    fun emitDetectorEvent(event: DetectorTrigger) {
        _detectorEvents.tryEmit(event)
    }

    fun emitResetEvent() {
        _resetEvents.tryEmit(Unit)
    }
}
