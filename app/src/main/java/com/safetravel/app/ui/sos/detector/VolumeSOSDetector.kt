package com.safetravel.app.ui.sos.detector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class VolumeSOSDetector(
    private val context: Context,
    private val onSosTriggered: () -> Unit
) {

    companion object {
        private const val TAG = "VolumeSOSDetector"
        private const val CLICK_COUNT_THRESHOLD = 10
        private const val TIME_WINDOW_MS = 10000L
        private const val VOLUME_BROADCAST_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    }

    private var clickCount = 0
    private var lastClickTime = 0L
    private var isListening = false
    private val lastVolumesByStream = mutableMapOf<Int, Int>() // Track per-stream changes

    private val resetHandler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable {
        clickCount = 0
    }

    private val volumeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == VOLUME_BROADCAST_ACTION) {
                    processVolumeChange(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing volume change", e)
            }
        }
    }

    /**
     * Start monitoring volume changes.
     */
    fun start() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        try {
            val filter = IntentFilter(VOLUME_BROADCAST_ACTION)

            // Register with proper flags for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    volumeBroadcastReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(volumeBroadcastReceiver, filter)
            }

            // Initialize last volume snapshots for the streams we care about
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            listOf(
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_VOICE_CALL
            ).forEach { stream ->
                lastVolumesByStream[stream] = audioManager?.getStreamVolume(stream) ?: -1
            }

            isListening = true
            Log.d(TAG, "Started listening for volume changes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            isListening = false
        }
    }

    /**
     * Stop monitoring. Important to call this to prevent memory leaks.
     */
    fun stop() {
        if (!isListening) {
            return
        }

        try {
            context.unregisterReceiver(volumeBroadcastReceiver)
            Log.d(TAG, "Stopped listening")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        } finally {
            isListening = false
            resetHandler.removeCallbacks(resetRunnable)
            clickCount = 0
        }
    }

    private fun processVolumeChange(intent: Intent) {
        val streamType = intent.getIntExtra(EXTRA_STREAM_TYPE, -1)
        val newVolume = intent.getIntExtra(EXTRA_STREAM_VALUE, -1)

        // Only act on real volume changes for common hardware buttons (music/ring/alarm/voice).
        val relevantStreams = setOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_VOICE_CALL
        )

        if (streamType !in relevantStreams || newVolume < 0) return

        val lastVolume = lastVolumesByStream[streamType]
        if (lastVolume != null && lastVolume == newVolume) return // no change
        lastVolumesByStream[streamType] = newVolume

        val now = System.currentTimeMillis()

        if (now - lastClickTime > TIME_WINDOW_MS) {
            clickCount = 1
        } else {
            clickCount++
        }

        lastClickTime = now

        Log.d(TAG, "Volume change detected: count=$clickCount")

        if (clickCount >= CLICK_COUNT_THRESHOLD) {
            Log.i(TAG, "SOS threshold reached!")
            onSosTriggered()
            clickCount = 0
        }

        resetHandler.removeCallbacks(resetRunnable)
        resetHandler.postDelayed(resetRunnable, TIME_WINDOW_MS)
    }
}
