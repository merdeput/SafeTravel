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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safetravel.app.MainActivity
import com.safetravel.app.R
import com.safetravel.app.data.repository.LocationService
import com.safetravel.app.data.repository.SensorDataRepository
import com.safetravel.app.ui.sos.detector.AccidentDetector
import com.safetravel.app.ui.sos.detector.PaperFallDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private var accidentDetector = AccidentDetector()
    private val paperFallDetector = PaperFallDetector()
    
    // Sensors
    private var linearAcceleration: Sensor? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var gravity: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var lastAccelerometerValues = FloatArray(3)

    // Channel to process sensor events sequentially and avoid concurrency issues
    private val sensorEventChannel = Channel<SensorEventData>(Channel.UNLIMITED)

    private data class SensorEventData(
        val sensorType: Int,
        val values: FloatArray,
        val timestamp: Long
    )
    
    companion object {
        const val ACTION_RESET_DETECTOR = "com.safetravel.app.action.RESET_DETECTOR"
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
                    showAccidentAlertNotification()
                }
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET_DETECTOR) {
            resetDetectors()
        } else {
            startForegroundService()
            startLocationTracking()
            registerSensors()
        }
        return START_STICKY
    }

    private fun resetDetectors() {
        accidentDetector.reset()
        paperFallDetector.reset()
    }

    private fun startForegroundService() {
        val channelId = "safety_background_channel"
        val channelName = "Safety Monitoring"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        )

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
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
    
    private fun showAccidentAlertNotification() {
        val channelId = "safety_alert_channel"
        val channelName = "Emergency Alerts"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
        )

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ACCIDENT DETECTED")
            .setContentText("Sending help in 30 seconds. Tap to cancel.")
            .setSmallIcon(R.mipmap.ic_launcher) // Changed to mipmap to avoid resource error
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun startLocationTracking() {
        // Request location updates every 10 seconds
        locationService.getLocationUpdates(10000L) 
            .onEach { location ->
                // Location updates are active. 
                // In a real app, you might save this to a DB or send to server.
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
        when (event.sensorType) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val result = accidentDetector.processAccelerometer(event.values, event.timestamp)
                // Push to repository so UI can observe
                sensorDataRepository.addSensorData(result)
                
                if (result.accidentConfirmed) {
                    // Alert is handled by the observer in onCreate
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometerValues, 0, 3)
                val isPaperFall = paperFallDetector.processAccelerometer(event.values, event.timestamp / 1000000)
                sensorDataRepository.updatePaperState(paperFallDetector.getCurrentStateName())

                if (isPaperFall) {
                    // TODO: Trigger Background Alert
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
    }
}
