package com.safetravel.app.ui.sos.detector

import com.safetravel.app.ui.sos.data.DetectionState
import com.safetravel.app.ui.sos.data.DetectionStateEnum
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class AccidentDetector {
    private var previousAccel = FloatArray(3)
    private var previousTimestamp = 0L

    // Stores the gravity vector from *before* the impact
    private var gravityAtRest = FloatArray(3) { 0f }
    private var isFirstGravityReading = true

    private val accelBuffer = mutableListOf<Float>()
    private val maxBufferSize = 50 // 1 second at 50Hz (approx 20ms per reading)

    private var currentState = DetectionStateEnum.IDLE
    private var impactStartTime = 0L
    private var validationStartTime = 0L

    private var currentGyro = FloatArray(3)
    private var currentGravity = FloatArray(3)

    fun processAccelerometer(values: FloatArray, timestamp: Long): DetectionState {
        // --- 1. Calculations ---

        val tam = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))

        val deltaTime = if (previousTimestamp > 0) {
            (timestamp - previousTimestamp) / 1_000_000_000.0 // Convert to seconds
        } else {
            0.02 // Default 50Hz
        }

        val jerk = if (previousTimestamp > 0 && deltaTime > 0) {
            val jx = (values[0] - previousAccel[0]) / deltaTime.toFloat()
            val jy = (values[1] - previousAccel[1]) / deltaTime.toFloat()
            val jz = (values[2] - previousAccel[2]) / deltaTime.toFloat()
            sqrt(jx.pow(2) + jy.pow(2) + jz.pow(2))
        } else {
            0f
        }

        accelBuffer.add(abs(values[0]) + abs(values[1]) + abs(values[2]))
        if (accelBuffer.size > maxBufferSize) {
            accelBuffer.removeAt(0)
        }
        val sma = if (accelBuffer.isNotEmpty()) accelBuffer.average().toFloat() else 0f

        val orientationChange = calculateOrientationChange(currentGravity, gravityAtRest)
        val angularMagnitude = sqrt(currentGyro[0].pow(2) + currentGyro[1].pow(2) + currentGyro[2].pow(2))

        // --- 2. State Machine Logic ---
        val now = System.currentTimeMillis()

        when (currentState) {
            DetectionStateEnum.IDLE -> {
                if (!isFirstGravityReading) {
                    currentState = DetectionStateEnum.MONITORING
                }
            }
            DetectionStateEnum.MONITORING -> {
                // REVISED: Trigger on SPIKE only.
                if (tam > 20f && jerk > 100f) {
                    // This is the impact. Store the "before" state.
                    gravityAtRest = currentGravity.copyOf()
                    currentState = DetectionStateEnum.POTENTIAL_IMPACT
                    impactStartTime = now
                }
            }
            DetectionStateEnum.POTENTIAL_IMPACT -> {
                val duration = now - impactStartTime

                // Wait 2 seconds for the event to finish
                if (duration > 2000) {
                    // REVISED: Check aftermath.
                    // Condition A: Did orientation change? (A fall/rollover)
                    val orientationChanged = orientationChange > 60f
                    // Condition B: Is the user motionless *right now*? (A crash/unconscious)
                    val isStillNow = sma < 1.0f

                    if (orientationChanged || isStillNow) {
                        // High confidence. Start final 30-second stillness check.
                        currentState = DetectionStateEnum.VALIDATING
                        validationStartTime = now
                    } else {
                        // False alarm. (e.g., dropped phone, picked it right up)
                        currentState = DetectionStateEnum.MONITORING
                    }
                    impactStartTime = 0L
                }
                // (Else: keep waiting for the 2-second window to pass)
            }
            DetectionStateEnum.VALIDATING -> {
                val elapsed = now - validationStartTime

                // Wait 30 seconds
                if (elapsed > 30000) {
                    // Final Check: Is the user *still* motionless?
                    if (sma < 1.0f) {
                        currentState = DetectionStateEnum.CONFIRMED_ACCIDENT
                    } else {
                        // User got up or is moving. False alarm.
                        currentState = DetectionStateEnum.MONITORING
                        validationStartTime = 0L
                    }
                }
            }
            DetectionStateEnum.CONFIRMED_ACCIDENT -> {
                // Stay in this state until reset from UI
            }
        }

        // --- 3. Update & Return ---
        previousAccel = values.copyOf()
        previousTimestamp = timestamp

        val impactDuration = if (impactStartTime > 0) now - impactStartTime else 0L

        return DetectionState(
            accelX = values[0],
            accelY = values[1],
            accelZ = values[2],
            tam = tam,
            jerk = jerk,
            gyroX = currentGyro[0],
            gyroY = currentGyro[1],
            gyroZ = currentGyro[2],
            angularMagnitude = angularMagnitude,
            gravityX = currentGravity[0],
            gravityY = currentGravity[1],
            gravityZ = currentGravity[2],
            orientationChange = orientationChange,
            sma = sma,
            impactDuration = impactDuration,
            currentState = currentState,
            accidentConfirmed = currentState == DetectionStateEnum.CONFIRMED_ACCIDENT,
            validationStartTime = validationStartTime
        )
    }

    fun processGyroscope(values: FloatArray) {
        currentGyro = values.copyOf()
    }

    fun processGravity(values: FloatArray) {
        currentGravity = values.copyOf()

        // REVISED: Only update "at rest" gravity if we are in IDLE
        // or if the user is not moving much (low SMA).
        // This prevents updating 'gravityAtRest' with a bad value
        // during normal, non-impact-related motion.
        val sma = if (accelBuffer.isNotEmpty()) accelBuffer.average().toFloat() else 0f

        if (isFirstGravityReading && values.any { it != 0f }) {
            gravityAtRest = values.copyOf()
            isFirstGravityReading = false
        } else if (currentState == DetectionStateEnum.MONITORING && sma < 1.5f) {
            // Continuously update "at rest" gravity while user is relatively still
            gravityAtRest = values.copyOf()
        }
    }

    private fun calculateOrientationChange(current: FloatArray, previous: FloatArray): Float {
        // Check if either vector is uninitialized
        if (previous.all { it == 0f } || current.all { it == 0f }) return 0f

        val dotProduct = current[0] * previous[0] + current[1] * previous[1] + current[2] * previous[2]
        val mag1 = sqrt(current[0].pow(2) + current[1].pow(2) + current[2].pow(2))
        val mag2 = sqrt(previous[0].pow(2) + previous[1].pow(2) + previous[2].pow(2))

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble()).toFloat().toDouble()).toFloat()
    }
}