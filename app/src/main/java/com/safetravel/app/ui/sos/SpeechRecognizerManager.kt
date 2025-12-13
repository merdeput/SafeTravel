package com.safetravel.app.ui.sos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var onResultCallback: ((String) -> Unit)? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            Log.e("SpeechRecognizer", "Speech recognition not available on this device")
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (speechRecognizer == null) {
            Log.e("SpeechRecognizer", "SpeechRecognizer is null")
            return
        }

        onResultCallback = onResult

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - can be used for visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                Log.e("SpeechRecognizer", "Error: $errorMessage (code: $error)")

                // Call the callback with empty string to signal completion
                // even on error (except for timeout/no match which are expected)
                if (error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                    error != SpeechRecognizer.ERROR_NO_MATCH) {
                    onResultCallback?.invoke("")
                } else {
                    // For timeout/no match, just invoke with empty to stop recording state
                    onResultCallback?.invoke("")
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcription = matches[0]
                    Log.d("SpeechRecognizer", "Result: $transcription")
                    onResultCallback?.invoke(transcription)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Can be used for real-time transcription display
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d("SpeechRecognizer", "Partial: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error starting recognition", e)
            onResultCallback?.invoke("")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error stopping recognition", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            onResultCallback = null
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error destroying recognizer", e)
        }
    }
}