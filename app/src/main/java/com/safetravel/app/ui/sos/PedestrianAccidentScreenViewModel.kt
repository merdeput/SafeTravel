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
import com.safetravel.app.ui.sos.detector.PaperFallDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedestrianAccidentScreenViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository
) : ViewModel() {

    // System 1 State
    private val _detectionState = MutableStateFlow(DetectionState())
    val detectionState = _detectionState.asStateFlow()

    // System 2 State
    private val _paperStateName = MutableStateFlow("IDLE")
    val paperStateName = _paperStateName.asStateFlow()

    // Combined Alert State
    private val _accidentDetected = MutableStateFlow(false)
    val accidentDetected = _accidentDetected.asStateFlow()

    private val _detectionTime = MutableStateFlow(0L)
    val detectionTime = _detectionTime.asStateFlow()

    // Countdown Logic State
    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive = _isCountdownActive.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(30)
    val countdownSeconds = _countdownSeconds.asStateFlow()

    private val _showPasscodeDialog = MutableStateFlow(false)
    val showPasscodeDialog = _showPasscodeDialog.asStateFlow()

    private val _passcodeError = MutableStateFlow<String?>(null)
    val passcodeError = _passcodeError.asStateFlow()

    private var countdownJob: Job? = null

    val sensorDataDeque = sensorDataRepository.sensorDataDeque

    // Detectors
    private var accidentDetector = AccidentDetector() // Changed to var to allow reset by recreation
    private val paperFallDetector = PaperFallDetector()

    private lateinit var sensorManager: SensorManager
    
    // Sensors
    private var linearAcceleration: Sensor? = null
    private var accelerometer: Sensor? = null // For Paper Detector (needs gravity included)
    private var gyroscope: Sensor? = null
    private var gravity: Sensor? = null
    private var magnetometer: Sensor? = null

    // Cache for Magnetometer processing
    private var lastAccelerometerValues = FloatArray(3)

    fun init(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        registerListeners()
    }

    // Listener for System 1 (Linear Accel)
    private val linearAccelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                viewModelScope.launch {
                    val result = accidentDetector.processAccelerometer(it.values, it.timestamp)
                    _detectionState.value = result
                    sensorDataRepository.addSensorData(result)

                    checkForAccident(result.accidentConfirmed, false)
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Listener for System 2 (Raw Accel)
    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                System.arraycopy(it.values, 0, lastAccelerometerValues, 0, 3)
                
                val isPaperFall = paperFallDetector.processAccelerometer(it.values, it.timestamp / 1000000)
                _paperStateName.value = paperFallDetector.getCurrentStateName()
                
                checkForAccident(false, isPaperFall)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                accidentDetector.processGyroscope(it.values, it.timestamp)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val gravityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                accidentDetector.processGravitySensor(it.values)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val magnetometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                paperFallDetector.processMagnetometer(it.values, lastAccelerometerValues)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun checkForAccident(system1Confirmed: Boolean, system2Confirmed: Boolean) {
        if ((system1Confirmed || system2Confirmed)) {
            // Start countdown only if not already active and accident not yet confirmed
            if (!_accidentDetected.value && !_isCountdownActive.value) {
                startCountdown()
            }
        }
    }

    private fun startCountdown() {
        _isCountdownActive.value = true
        _countdownSeconds.value = 30
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 30 downTo 1) {
                _countdownSeconds.value = i
                delay(1000)
            }
            // Timeout reached -> Confirm Accident
            confirmAccident()
        }
    }

    private fun confirmAccident() {
        _isCountdownActive.value = false
        _showPasscodeDialog.value = false
        _accidentDetected.value = true
        _detectionTime.value = System.currentTimeMillis()
    }

    fun onImOkayClick() {
        _showPasscodeDialog.value = true
        _passcodeError.value = null
    }

    fun onVerifyPasscode(passcode: String) {
        if (passcode == "1234") { // Dummy passcode
            countdownJob?.cancel()
            _isCountdownActive.value = false
            _showPasscodeDialog.value = false
            _accidentDetected.value = false
            
            // Reset logic
            paperFallDetector.reset()
            accidentDetector = AccidentDetector() // Re-instantiate to reset state
        } else {
            _passcodeError.value = "Invalid passcode"
        }
    }
    
    fun onPasscodeDialogDismiss() {
        _showPasscodeDialog.value = false
    }

    fun onSendHelpClick() {
        countdownJob?.cancel()
        confirmAccident()
    }

    private fun registerListeners() {
        linearAcceleration?.let { sensorManager.registerListener(linearAccelListener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        accelerometer?.let { sensorManager.registerListener(accelerometerListener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gyroscope?.let { sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gravity?.let { sensorManager.registerListener(gravityListener, it, SensorManager.SENSOR_DELAY_FASTEST) }
        magnetometer?.let { sensorManager.registerListener(magnetometerListener, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    private fun unregisterListeners() {
        sensorManager.unregisterListener(linearAccelListener)
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(gravityListener)
        sensorManager.unregisterListener(magnetometerListener)
    }

    fun onReset() {
        _accidentDetected.value = false
        _detectionTime.value = 0L
        paperFallDetector.reset()
        accidentDetector = AccidentDetector() // Re-instantiate to reset state
        
        _isCountdownActive.value = false
        _showPasscodeDialog.value = false
        countdownJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        unregisterListeners()
        countdownJob?.cancel()
    }
}
