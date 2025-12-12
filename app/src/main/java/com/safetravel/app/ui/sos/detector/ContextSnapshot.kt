package com.safetravel.app.ui.sos.detector

import com.safetravel.app.ui.sos.data.ActivityHint

/**
 * Lightweight context passed into the detector for dynamic thresholding.
 */
data class ContextSnapshot(
    val speedMps: Float = 0f,
    val activityHint: ActivityHint = ActivityHint.UNKNOWN
)
