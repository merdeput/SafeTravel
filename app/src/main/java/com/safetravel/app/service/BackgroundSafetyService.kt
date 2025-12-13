package com.safetravel.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.safetravel.app.MainActivity
import com.safetravel.app.R
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.LocationService
import com.safetravel.app.data.repository.SensorDataRepository
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.data.repository.SosRepository
import com.safetravel.app.ui.sos.EmergencyActivity
import com.safetravel.app.ui.sos.data.ActivityHint
import com.safetravel.app.ui.sos.detector.ContextSnapshot
import com.safetravel.app.ui.sos.detector.AccidentDetector
import com.safetravel.app.ui.sos.detector.PaperFallDetector
import com.safetravel.app.ui.sos.detector.VolumeSOSDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundSafetyService : Service(), SensorEventListener {

    @Inject
    lateinit var locationService: LocationService

    @Inject
    lateinit var sensorDataRepository: SensorDataRepository

    @Inject
    lateinit var sosRepository: SosRepository

    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private var accidentDetector = AccidentDetector()
    private val paperFallDetector = PaperFallDetector()
    private lateinit var volumeSOSDetector: VolumeSOSDetector
    
    // Sensors
    private var linearAcceleration: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gravity: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var lastAccelerometerValues = FloatArray(3)
    private var latestSpeedMps = 0f
    private var lastLocation: Location? = null
    private var lastLocationTimestampMs: Long = 0L
    private var latestActivityHint: ActivityHint = ActivityHint.UNKNOWN

    // Channel to process sensor events sequentially and avoid concurrency issues
    private val sensorEventChannel = Channel<SensorEventData>(Channel.UNLIMITED)
    
    // Job for the alert countdown
    private var alertCountdownJob: Job? = null

    // Constant vibration job
    private var vibrationJob: Job? = null

    // Flags to avoid duplicate triggers from the same incident
    private var detectorTriggerActive = false // Covers accident/fall detectors
    private var volumeTriggerSent = false

    private data class SensorEventData(
        val sensorType: Int,
        val values: FloatArray,
        val timestamp: Long
    )
    
    companion object {
        const val ACTION_RESET_DETECTOR = "com.safetravel.app.action.RESET_DETECTOR"
        const val ACTION_ALERT_SENT = "com.safetravel.app.action.ALERT_SENT"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        // Initialize Volume SOS Detector
        volumeSOSDetector = VolumeSOSDetector(this) {
            if (!volumeTriggerSent) {
                volumeTriggerSent = true
                sensorDataRepository.emitDetectorEvent(SensorDataRepository.DetectorTrigger.VolumeSos)
            }
            showAccidentAlertNotification()
        }
        volumeSOSDetector.start()

        // Start processing sensor events sequentially
        serviceScope.launch {
            for (event in sensorEventChannel) {
                processSensorEvent(event)
            }
        }
        
        // Observe detection state to trigger alerts
        sensorDataRepository.latestDetectionState
            .onEach { state ->
                if (state.accidentConfirmed) {
                    // Ensure we don't start multiple countdowns for the same event
                    if (alertCountdownJob?.isActive != true) {
                        showAccidentAlertNotification()
                    }
                }
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESET_DETECTOR -> resetDetectors()
            ACTION_ALERT_SENT -> showAlertSentNotification()
            else -> {
                startForegroundService()
                startLocationTracking()
                registerSensors()
            }
        }
        return START_STICKY
    }

    private fun resetDetectors() {
        accidentDetector.reset()
        paperFallDetector.reset()
        detectorTriggerActive = false
        volumeTriggerSent = false
        latestSpeedMps = 0f
        lastLocation = null
        lastLocationTimestampMs = 0L
        latestActivityHint = ActivityHint.UNKNOWN
        
        // Stop vibration immediately
        stopVibration()

        // Cancel any pending alert countdown
        alertCountdownJob?.cancel()
        alertCountdownJob = null
        
        // Clear notifications if needed (optional, but good UX)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(2) // Cancel accident notification
        notificationManager.cancel(3) // Cancel fall notification
    }

    private fun startForegroundService() {
        val channelId = "safety_background_channel"
        val channelName = "Safety Monitoring"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        )

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safe Travel Active")
            .setContentText("Monitoring started at $startTime")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setTicker("Safe Travel Service Running") // For accessibility
            .build()

        startForeground(1, notification)
    }
    
    private fun startConstantVibration() {
        vibrationJob?.cancel() // Cancel any existing job
        vibrationJob = serviceScope.launch {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                while (true) { // Loop indefinitely until cancelled
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Vibrate for 1 second
                        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(1000)
                    }
                    // Wait for 1 second vibrate + 0.5 second pause
                    delay(1500) 
                }
            }
        }
    }

    private fun stopVibration() {
        vibrationJob?.cancel()
        vibrationJob = null
        
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    private fun showAccidentAlertNotification() {
        // Start constant vibration
        startConstantVibration()

        val channelId = "safety_alert_channel"
        val channelName = "Emergency Alerts"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )

        val pendingIntent: PendingIntent = Intent(this, EmergencyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }.let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ACCIDENT DETECTED")
            .setContentText("Tap to cancel or view emergency info.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // This wakes up the screen and launches activity
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
        
        // Start countdown to send alert
        startAlertCountdown("Vehicle accident detected")
    }
    
    private fun startAlertCountdown(message: String) {
        alertCountdownJob?.cancel()
        alertCountdownJob = serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val countdown = settings.countdownTime.toLong() * 1000
            
            delay(countdown) // User configured delay
            
            // Stop vibration before sending alert (or keep it going until user dismisses?)
            // Usually we stop it when the action is taken or dismissed. 
            // For now, let's keep it until 'resetDetectors' or 'showAlertSentNotification' stops it.
            stopVibration() 

            val location = locationService.getCurrentLocation()
            val user = authRepository.currentUser
            val userId = user?.id

            if (user != null && location != null && userId != null) {
                try {
                    sosRepository.sendSos(
                        userId = userId,
                        circleId = null,
                        message = message,
                        lat = location.latitude,
                        lng = location.longitude
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            showAlertSentNotification()
        }
    }

    private fun showFallAlertNotification() {
        // Start constant vibration
        startConstantVibration()

        val channelId = "safety_alert_channel"
        val channelName = "Emergency Alerts"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )

        val pendingIntent: PendingIntent = Intent(this, EmergencyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }.let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FALL DETECTED")
            .setContentText("A fall was detected. Tap to cancel or view info.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true) // Wake up screen and launch activity
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)

        startAlertCountdown("Fall detected")
    }

    private fun showAlertSentNotification() {
        // Ensure vibration is stopped when the alert is finally sent
        stopVibration()

        val channelId = "safety_alert_channel"
        val channelName = "Emergency Alerts"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ALERT SENT")
            .setContentText("Emergency contacts have been notified.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4, notification)
    }

    private fun startLocationTracking() {
        // Request location updates every 10 seconds
        locationService.getLocationUpdates(2000L)
            .onEach { location ->
                updateSpeedFromLocation(location)
            }
            .launchIn(serviceScope)
    }

    private fun registerSensors() {
        // Using SENSOR_DELAY_GAME (20ms) as a balance for background processing
        linearAcceleration?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        // Copy data immediately to avoid recycling issues and concurrency
        val data = SensorEventData(event.sensor.type, event.values.clone(), event.timestamp)
        sensorEventChannel.trySend(data)
    }

    private suspend fun processSensorEvent(event: SensorEventData) {
        val contextSnapshot = currentContextSnapshot()
        when (event.sensorType) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val result = accidentDetector.processAccelerometer(event.values, event.timestamp, contextSnapshot)
                // Push to repository so UI can observe
                sensorDataRepository.addSensorData(result)
                
                if (result.accidentConfirmed) {
                    if (!detectorTriggerActive) {
                        detectorTriggerActive = true
                        sensorDataRepository.emitDetectorEvent(SensorDataRepository.DetectorTrigger.Accident)
                    }
                    // Alert is handled by the observer in onCreate
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometerValues, 0, 3)
                val isPaperFall = paperFallDetector.processAccelerometer(event.values, event.timestamp / 1000000)
                sensorDataRepository.updatePaperState(paperFallDetector.getCurrentStateName())

                if (isPaperFall && !detectorTriggerActive) {
                    detectorTriggerActive = true
                    sensorDataRepository.emitDetectorEvent(SensorDataRepository.DetectorTrigger.Fall)
                    showFallAlertNotification()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                accidentDetector.processGyroscope(event.values, event.timestamp)
            }
            Sensor.TYPE_GRAVITY -> {
                accidentDetector.processGravitySensor(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                paperFallDetector.processMagnetometer(event.values, lastAccelerometerValues)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        if (::volumeSOSDetector.isInitialized) {
            volumeSOSDetector.stop()
        }
    }

    private fun updateSpeedFromLocation(location: Location) {
        val nowMs = System.currentTimeMillis()
        val computedSpeed = if (location.hasSpeed()) {
            location.speed
        } else {
            val lastLoc = lastLocation
            if (lastLoc != null) {
                val distance = lastLoc.distanceTo(location) // meters
                val timeDeltaSec = ((nowMs - lastLocationTimestampMs).coerceAtLeast(1L)) / 1000f
                distance / timeDeltaSec
            } else {
                0f
            }
        }
        latestSpeedMps = computedSpeed.coerceAtLeast(0f)
        lastLocation = location
        lastLocationTimestampMs = nowMs
        latestActivityHint = inferActivityHint(latestSpeedMps)
    }

    private fun currentContextSnapshot(): ContextSnapshot {
        return ContextSnapshot(
            speedMps = latestSpeedMps,
            activityHint = latestActivityHint
        )
    }

    private fun inferActivityHint(speedMps: Float): ActivityHint {
        return when {
            speedMps > 6f -> ActivityHint.VEHICLE
            speedMps > 2.5f -> ActivityHint.RUNNING
            speedMps > 0.8f -> ActivityHint.WALKING
            speedMps >= 0f -> ActivityHint.STATIONARY
            else -> ActivityHint.UNKNOWN
        }
    }
}
