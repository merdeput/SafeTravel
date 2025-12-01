package com.safetravel.app.ui.sos.detector

import com.safetravel.app.ui.sos.data.DetectionState
import com.safetravel.app.ui.sos.data.DetectionStateEnum
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class AccidentDetector {
    // --- Configuration Constants ---
    companion object {
        // Thresholds
        private const val THRESHOLD_IMPACT_TAM = 24.0f // ~2.4g (High force)
        private const val THRESHOLD_IMPACT_JERK = 150.0f // Sudden jolt
        private const val THRESHOLD_TUMBLE_TAM = 18.0f // ~1.8g (Moderate force)
        private const val THRESHOLD_TUMBLE_GYRO = 4.0f // ~230 deg/s (in radians) or raw gyro magnitude
        private const val THRESHOLD_STILLNESS_SMA = 1.0f // "At rest" threshold
        private const val THRESHOLD_MOVEMENT_CANCEL = 6.0f // Movement high enough to cancel alarm (increased)
        private const val THRESHOLD_ORIENTATION_CHANGE = 45.0f // Degrees
        
        // Timings
        private const val TIME_IMPACT_WINDOW_MS = 3000L // 3 seconds to settle (increased)
        private const val TIME_VALIDATION_WINDOW_MS = 15000L // 15 seconds to confirm (reduced)
        private const val TIME_GYRO_SYNC_TOLERANCE_MS = 100L // Gyro data must be within 100ms
        
        // Buffer settings
        private const val BUFFER_SIZE_NORMAL = 50 // 1 second at 50Hz
        private const val BUFFER_SIZE_POST_IMPACT = 25 // 0.5 second for post-impact analysis
    }

    // --- Variables ---
    private var previousAccel = FloatArray(3)
    private var previousTimestamp = 0L
    
    // Gravity Vectors
    private var referenceGravity = FloatArray(3) { 0f } // The "Before" state
    private var currentGravity = FloatArray(3) // The "Now" state (Low-pass filtered)
    private var isReferenceSet = false
    private var isReferenceLocked = false // NEW: Prevents reference updates during detection
    
    // Buffers
    private val accelBuffer = mutableListOf<Float>()
    private var maxBufferSize = BUFFER_SIZE_NORMAL
    
    // State
    private var currentState = DetectionStateEnum.IDLE
    private var impactStartTime = 0L
    private var validationStartTime = 0L
    
    // Sensor Inputs
    private var currentGyro = FloatArray(3)
    private var lastGyroTimestamp = 0L // NEW: Track gyro data freshness
    
    // NEW: Movement tracking for smarter cancellation
    private var significantMovementCount = 0
    private val movementThreshold = 3 // Need 3 significant movements to cancel

    fun processAccelerometer(values: FloatArray, timestamp: Long): DetectionState {
        // --- 1. Physics Calculations ---
        
        // A. Total Acceleration Magnitude (TAM)
        val tam = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
        
        // B. Time Delta
        val deltaTime = if (previousTimestamp > 0) {
            (timestamp - previousTimestamp) / 1_000_000_000.0 // Nanoseconds to Seconds
        } else {
            0.02 // Default to 50Hz (20ms) if first reading
        }
        
        // C. Jerk Calculation
        val jerk = if (previousTimestamp > 0 && deltaTime > 0) {
            val jx = (values[0] - previousAccel[0]) / deltaTime.toFloat()
            val jy = (values[1] - previousAccel[1]) / deltaTime.toFloat()
            val jz = (values[2] - previousAccel[2]) / deltaTime.toFloat()
            sqrt(jx.pow(2) + jy.pow(2) + jz.pow(2))
        } else {
            0f
        }
        
        // D. SMA (Signal Magnitude Area) - Activity Level
        val currentMagnitude = abs(values[0]) + abs(values[1]) + abs(values[2])
        accelBuffer.add(currentMagnitude)
        if (accelBuffer.size > maxBufferSize) {
            accelBuffer.removeAt(0)
        }
        val sma = if (accelBuffer.isNotEmpty()) accelBuffer.average().toFloat() else 0f
        
        // E. Gyro Magnitude with Freshness Check
        val gyroAge = timestamp - lastGyroTimestamp
        val isGyroFresh = gyroAge < TIME_GYRO_SYNC_TOLERANCE_MS * 1_000_000 // Convert to nanoseconds
        val gyroMagnitude = if (isGyroFresh) {
            sqrt(currentGyro[0].pow(2) + currentGyro[1].pow(2) + currentGyro[2].pow(2))
        } else {
            0f // Ignore stale gyro data
        }
        
        // F. Low-Pass Filter for Gravity Extraction
        val alpha = 0.1f
        currentGravity[0] = alpha * values[0] + (1 - alpha) * currentGravity[0]
        currentGravity[1] = alpha * values[1] + (1 - alpha) * currentGravity[1]
        currentGravity[2] = alpha * values[2] + (1 - alpha) * currentGravity[2]
        
        // --- 2. State Machine ---
        val now = System.currentTimeMillis()
        
        // Calculate orientation change
        val orientationChange = if (isReferenceSet && isReferenceLocked) {
            calculateOrientationChange(currentGravity, referenceGravity)
        } else {
            0f
        }
        
        when (currentState) {
            DetectionStateEnum.IDLE -> {
                // Initial stabilization
                if (accelBuffer.size >= maxBufferSize) {
                    currentState = DetectionStateEnum.MONITORING
                }
            }
            
            DetectionStateEnum.MONITORING -> {
                // Update Reference Gravity ONLY when still and not locked
                if (sma < THRESHOLD_STILLNESS_SMA && !isReferenceLocked) {
                    referenceGravity = currentGravity.copyOf()
                    isReferenceSet = true
                }
                
                // TRIGGER LOGIC
                // 1. Hard Impact: High G + High Jerk
                val isHardImpact = tam > THRESHOLD_IMPACT_TAM && jerk > THRESHOLD_IMPACT_JERK
                
                // 2. Tumble: Moderate G + High Rotation (only if gyro is fresh)
                val isTumble = tam > THRESHOLD_TUMBLE_TAM && 
                              gyroMagnitude > THRESHOLD_TUMBLE_GYRO && 
                              isGyroFresh
                
                if (isHardImpact || isTumble) {
                    // LOCK the reference gravity to preserve "before" state
                    isReferenceLocked = true
                    currentState = DetectionStateEnum.POTENTIAL_IMPACT
                    impactStartTime = now
                    
                    // Switch to smaller buffer for more responsive post-impact analysis
                    maxBufferSize = BUFFER_SIZE_POST_IMPACT
                    accelBuffer.clear() // Clear impact spike from buffer
                    
                    // Reset movement counter
                    significantMovementCount = 0
                }
            }
            
            DetectionStateEnum.POTENTIAL_IMPACT -> {
                val duration = now - impactStartTime
                
                if (duration > TIME_IMPACT_WINDOW_MS) {
                    // Window closed. Analyze the aftermath.
                    
                    // Check 1: Is user motionless?
                    val isStill = sma < THRESHOLD_STILLNESS_SMA
                    
                    // Check 2: Did orientation change significantly?
                    val isOrientationChanged = isReferenceSet && 
                                              (orientationChange > THRESHOLD_ORIENTATION_CHANGE)
                    
                    if (isStill || isOrientationChanged) {
                        currentState = DetectionStateEnum.VALIDATING
                        validationStartTime = now
                    } else {
                        // User is moving actively and orientation unchanged
                        resetToMonitoring()
                    }
                }
            }
            
            DetectionStateEnum.VALIDATING -> {
                val elapsed = now - validationStartTime
                
                // IMPROVED CANCEL CONDITION:
                // Count significant movements instead of canceling on first movement
                if (sma > THRESHOLD_MOVEMENT_CANCEL) {
                    significantMovementCount++
                    
                    // Only cancel if there are multiple sustained movements
                    if (significantMovementCount >= movementThreshold) {
                        resetToMonitoring()
                    }
                } else {
                    // Reset counter if user returns to stillness
                    // This allows for brief fidgeting without canceling
                    if (significantMovementCount > 0) {
                        significantMovementCount--
                    }
                }
                
                // TIMEOUT CONDITION:
                if (elapsed > TIME_VALIDATION_WINDOW_MS && currentState == DetectionStateEnum.VALIDATING) {
                    // User has been relatively still for validation period
                    currentState = DetectionStateEnum.CONFIRMED_ACCIDENT
                }
            }
            
            DetectionStateEnum.CONFIRMED_ACCIDENT -> {
                // Terminal state. Waiting for UI to acknowledge/reset.
            }
        }
        
        // --- 3. Final Updates ---
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
            angularMagnitude = gyroMagnitude,
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
    
    fun processGyroscope(values: FloatArray, timestamp: Long) {
        currentGyro = values.copyOf()
        lastGyroTimestamp = timestamp
    }
    
    fun processGravitySensor(values: FloatArray) {
        // Only update current gravity if reference is not locked
        if (!isReferenceLocked) {
            currentGravity = values.copyOf()
        }
    }
    
    private fun resetToMonitoring() {
        currentState = DetectionStateEnum.MONITORING
        impactStartTime = 0L
        validationStartTime = 0L
        isReferenceLocked = false // Unlock reference for new baseline
        maxBufferSize = BUFFER_SIZE_NORMAL // Restore normal buffer size
        accelBuffer.clear() // Clean slate
        significantMovementCount = 0
    }
    
    private fun calculateOrientationChange(current: FloatArray, previous: FloatArray): Float {
        // Validation to prevent NaN
        if (previous.all { it == 0f } || current.all { it == 0f }) return 0f
        
        val dotProduct = current[0] * previous[0] + 
                        current[1] * previous[1] + 
                        current[2] * previous[2]
        
        val mag1 = sqrt(current[0].pow(2) + current[1].pow(2) + current[2].pow(2))
        val mag2 = sqrt(previous[0].pow(2) + previous[1].pow(2) + previous[2].pow(2))
        
        if (mag1 == 0f || mag2 == 0f) return 0f
        
        val cosAngle = (dotProduct / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosAngle.toDouble())).toFloat()
    }
}