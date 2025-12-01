package com.safetravel.app.ui.sos.detector

import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Fall Detection System based on the Cascade Classifier approach.
 * Source: "A Smart Phone-Based Pocket Fall Accident Detection..." (Kau & Chen, 2014)
 *
 * Logic Stages:
 * 1. Free Fall: Signal Magnitude Vector (SMV) drops below 0.6G.
 * 2. Impact: SMV spikes above 1.8G within 1.6 seconds of free fall.
 * 3. Stillness: Standard Deviation of SMV < 0.1G (User is motionless).
 * 4. Orientation: Pitch angle < 50 degrees (User is lying down).
 */
class PaperFallDetector {

    // --- Constants based on the Paper ---
    companion object {
        // Stage 1 & 2: Thresholds
        private const val THRESHOLD_FREE_FALL = 0.6f * 9.81f // Paper uses G, Android uses m/s^2
        private const val THRESHOLD_IMPACT = 1.8f * 9.81f

        // Stage 3: Variance Threshold
        private const val THRESHOLD_SIGMA = 0.1f * 9.81f // Standard deviation limit

        // Stage 4: Orientation Threshold
        private const val THRESHOLD_PITCH_DEG = 50.0f

        // Time Windows (Converted from Paper's 150Hz sample counts)
        // Paper: 250 samples @ 150Hz = ~1666ms window to find impact after free fall
        private const val TIME_WINDOW_IMPACT_MS = 1600L

        // Paper: 50 samples @ 150Hz = ~333ms for stillness check
        private const val TIME_WINDOW_STILLNESS_MS = 333L
    }

    // --- Internal State Enum ---
    enum class PaperState {
        IDLE,               // Waiting for Free Fall
        FREE_FALL_DETECTED, // Stage 1 passed, looking for Impact
        IMPACT_DETECTED,    // Stage 2 passed, collecting data for Stillness
        CONFIRMED           // All stages passed
    }

    // --- Variables ---
    private var currentState = PaperState.IDLE
    private var stateStartTime = 0L

    // Sensor Data Holders
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var currentPitch = 0f

    // Buffers for Stage 3 & 4
    private val stillnessBuffer = mutableListOf<Float>() // Stores SMV for StdDev calc
    private val pitchBuffer = mutableListOf<Float>()     // Stores Pitch for average calc

    /**
     * Main processing function. Call this every time you get an ACCELEROMETER event.
     * Returns TRUE if a fall is confirmed.
     */
    fun processAccelerometer(values: FloatArray, timestampMs: Long): Boolean {
        // 1. Calculate Signal Magnitude Vector (SMV)
        val smv = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))

        val now = System.currentTimeMillis()

        when (currentState) {
            // --- STAGE 1: Free Fall Detection ---
            PaperState.IDLE -> {
                if (smv < THRESHOLD_FREE_FALL) {
                    currentState = PaperState.FREE_FALL_DETECTED
                    stateStartTime = now
                }
            }

            // --- STAGE 2: Impact Detection ---
            PaperState.FREE_FALL_DETECTED -> {
                val elapsed = now - stateStartTime

                // Timeout Check: If no impact within window, reset.
                if (elapsed > TIME_WINDOW_IMPACT_MS) {
                    reset()
                    return false
                }

                // Impact Trigger
                if (smv > THRESHOLD_IMPACT) {
                    currentState = PaperState.IMPACT_DETECTED
                    stateStartTime = now
                    stillnessBuffer.clear()
                    pitchBuffer.clear()
                }
            }

            // --- STAGE 3 & 4: Stillness & Orientation ---
            PaperState.IMPACT_DETECTED -> {
                val elapsed = now - stateStartTime

                // Collect data for the "Aftermath" window
                if (elapsed < TIME_WINDOW_STILLNESS_MS) {
                    stillnessBuffer.add(smv)
                    pitchBuffer.add(abs(currentPitch))
                } else {
                    // Window filled. Perform Checks.

                    // Check A: Stillness (Standard Deviation)
                    val sigma = calculateStandardDeviation(stillnessBuffer)
                    val isStill = sigma < THRESHOLD_SIGMA

                    // Check B: Orientation (Average Pitch)
                    val avgPitch = if (pitchBuffer.isNotEmpty()) pitchBuffer.average() else 180.0
                    val isLyingDown = avgPitch < THRESHOLD_PITCH_DEG

                    // Final Decision
                    if (isStill && isLyingDown) {
                        currentState = PaperState.CONFIRMED
                        return true
                    } else {
                        // Failed validation (user is moving or upright)
                        reset()
                    }
                }
            }

            PaperState.CONFIRMED -> {
                // Already confirmed. External system should handle the alert and then call reset().
                return true
            }
        }

        return false
    }

    /**
     * Helper to process Magnetometer data for Orientation (Stage 4).
     * Call this when you get a MAGNETOMETER event.
     */
    fun processMagnetometer(values: FloatArray, accelValues: FloatArray) {
        System.arraycopy(values, 0, geomagnetic, 0, 3)
        System.arraycopy(accelValues, 0, gravity, 0, 3)

        // Android Sensor Fusion to get Rotation Matrix
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // orientationAngles[1] is Pitch (in Radians)
            // Convert to Degrees for the algorithm
            currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        }
    }

    private fun calculateStandardDeviation(buffer: List<Float>): Float {
        if (buffer.isEmpty()) return 0f
        val mean = buffer.average()
        val sumSquaredDiff = buffer.fold(0.0) { acc, value -> acc + (value - mean).pow(2) }
        return sqrt(sumSquaredDiff / buffer.size).toFloat()
    }

    fun reset() {
        currentState = PaperState.IDLE
        stillnessBuffer.clear()
        pitchBuffer.clear()
    }

    fun getCurrentStateName(): String {
        return currentState.name
    }
}