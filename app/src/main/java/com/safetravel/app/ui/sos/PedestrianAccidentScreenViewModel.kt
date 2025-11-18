package com.safetravel.app.ui.sos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.SensorDataRepository
import com.safetravel.app.ui.sos.data.DetectionState
import com.safetravel.app.ui.sos.detector.AccidentDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedestrianAccidentScreenViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository
) : ViewModel() {

    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState = _detectionState.asStateFlow()

    private val _accidentDetected = MutableStateFlow(false)
    val accidentDetected = _accidentDetected.asStateFlow()

    private val _detectionTime = MutableStateFlow(0L)
    val detectionTime = _detectionTime.asStateFlow()

    val sensorDataDeque = sensorDataRepository.sensorDataDeque

    private val detector = AccidentDetector()

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gravity: Sensor? = null

    fun init(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        registerListeners()
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                viewModelScope.launch {
                    val result = detector.processAccelerometer(it.values, it.timestamp)
                    _detectionState.value = result
                    sensorDataRepository.addSensorData(result)

                    if (result.accidentConfirmed && !_accidentDetected.value) {
                        _accidentDetected.value = true
                        _detectionTime.value = System.currentTimeMillis()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                detector.processGyroscope(it.values)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gravityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                detector.processGravity(it.values)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun registerListeners() {
        accelerometer?.let {
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gyroscope?.let {
            sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        gravity?.let {
            sensorManager.registerListener(gravityListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun unregisterListeners() {
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(gravityListener)
    }

    fun onReset() {
        _accidentDetected.value = false
        _detectionTime.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        unregisterListeners()
    }
}
