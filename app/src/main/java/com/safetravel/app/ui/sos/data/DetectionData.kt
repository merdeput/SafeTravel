package com.safetravel.app.ui.sos.data

import com.safetravel.app.ui.sos.data.ActivityHint

data class DetectionState(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val tam: Float = 0f,
    val jerk: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val angularMagnitude: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val orientationChange: Float = 0f,
    val sma: Float = 0f,
    val speedMps: Float = 0f,
    val speedDeltaMps: Float = 0f,
    val activityHint: ActivityHint = ActivityHint.UNKNOWN,
    val impactDuration: Long = 0L,
    val currentState: DetectionStateEnum = DetectionStateEnum.IDLE,
    val accidentConfirmed: Boolean = false,
    val validationStartTime: Long = 0L,
    val hasSpeedDrop: Boolean = false,
    val speedDropPercent: Float = 0f,
    val isGyroFresh: Boolean = false
)

enum class DetectionStateEnum {
    IDLE,
    MONITORING,
    POTENTIAL_IMPACT,
    VALIDATING,
    CONFIRMED_ACCIDENT
}
