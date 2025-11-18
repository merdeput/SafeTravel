package com.safetravel.app.data.repository

import com.safetravel.app.ui.sos.data.DetectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_DEQUE_SIZE = 300

@Singleton
class SensorDataRepository @Inject constructor() {

    private val _sensorDataDeque = MutableStateFlow<ArrayDeque<DetectionState>>(ArrayDeque())
    val sensorDataDeque = _sensorDataDeque.asStateFlow()

    fun addSensorData(state: DetectionState) {
        val newDeque = ArrayDeque(_sensorDataDeque.value)

        if (newDeque.size >= MAX_DEQUE_SIZE) {
            newDeque.removeFirst()
        }
        newDeque.addLast(state)

        _sensorDataDeque.value = newDeque
    }
}