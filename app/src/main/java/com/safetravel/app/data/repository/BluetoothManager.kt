package com.safetravel.app.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothBleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanRepository: BluetoothScanRepository
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    
    // UUID for SafeTravel SOS
    private val SERVICE_UUID = ParcelUuid(UUID.fromString("25AE1441-05D3-4C5B-8281-93D4E07420CF")) 

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d("BluetoothBleManager", "BLE Advertising started successfully")
            _isAdvertising.value = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("BluetoothBleManager", "BLE Advertising failed: $errorCode")
            _isAdvertising.value = false
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val scanRecord = it.scanRecord ?: return@let
                
                // Strictly verify that the device has our specific Service UUID or Service Data
                val hasServiceUuid = scanRecord.serviceUuids?.contains(SERVICE_UUID) == true
                val serviceData = scanRecord.getServiceData(SERVICE_UUID)
                val hasServiceData = serviceData != null
                
                if (hasServiceUuid || hasServiceData) {
                    val address = it.device.address
                    val rssi = it.rssi
                    
                    var message: String? = null
                    if (serviceData != null) {
                        message = String(serviceData, Charset.forName("UTF-8"))
                    }

                    Log.d("BluetoothBleManager", "Found SafeTravel SOS: $address, RSSI: $rssi, Msg: $message")
                    scanRepository.addOrUpdateSignal(address, rssi, message)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                val scanRecord = result.scanRecord ?: return@forEach
                val hasServiceUuid = scanRecord.serviceUuids?.contains(SERVICE_UUID) == true
                val serviceData = scanRecord.getServiceData(SERVICE_UUID)
                
                if (hasServiceUuid || serviceData != null) {
                    val message = serviceData?.let { bytes -> String(bytes, Charset.forName("UTF-8")) }
                    scanRepository.addOrUpdateSignal(result.device.address, result.rssi, message)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothBleManager", "Scan failed: $errorCode")
            scanRepository.setScanningState(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(message: String = "SOS!") {
        if (adapter == null || !adapter.isEnabled) {
            Log.e("BluetoothBleManager", "Bluetooth is off or not supported")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e("BluetoothBleManager", "BLE Advertising not supported on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Truncate message to fit (keep it short for BLE packet limits)
        val safeMessage = message.take(8)
        val messageBytes = safeMessage.toByteArray(Charset.forName("UTF-8"))

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            // Put message in Service Data associated with our UUID
            // We DO NOT add addServiceUuid(SERVICE_UUID) here because that would duplicate the 16-byte UUID 
            // in the packet, causing it to exceed the 31-byte legacy limit.
            // Scanners must scan for Service Data, not just Service UUID list.
            .addServiceData(SERVICE_UUID, messageBytes)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("BluetoothBleManager", "SecurityException during advertising", e)
        } catch (e: Exception) {
            Log.e("BluetoothBleManager", "Error starting advertising", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            Log.d("BluetoothBleManager", "BLE Advertising stopped")
        } catch (e: SecurityException) {
            Log.e("BluetoothBleManager", "SecurityException stopping advertising", e)
        } catch (e: Exception) {
            Log.e("BluetoothBleManager", "Error stopping advertising", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (adapter == null || !adapter.isEnabled) {
            Log.e("BluetoothBleManager", "Bluetooth is off or not supported")
            return
        }
        
        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
             Log.e("BluetoothBleManager", "BLE Scanning not supported on this device")
             return
        }
        
        // Filter by Service Data since that's where we put our UUID in the advertisement
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceData(SERVICE_UUID, null) // Match any data as long as UUID matches
                .build()
        )
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        try {
            scanner?.startScan(filters, settings, scanCallback)
            scanRepository.setScanningState(true)
            Log.d("BluetoothBleManager", "BLE Scanning started")
        } catch (e: SecurityException) {
            Log.e("BluetoothBleManager", "SecurityException during scanning", e)
        } catch (e: Exception) {
            Log.e("BluetoothBleManager", "Error starting scanning", e)
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
            scanRepository.setScanningState(false)
            Log.d("BluetoothBleManager", "BLE Scanning stopped")
        } catch (e: SecurityException) {
            Log.e("BluetoothBleManager", "SecurityException stopping scanning", e)
        } catch (e: Exception) {
            Log.e("BluetoothBleManager", "Error stopping scanning", e)
        }
    }
}
