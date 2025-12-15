package com.safetravel.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    // Define keys for the preferences
    private object PreferenceKeys {
        val COUNTDOWN_TIME = intPreferencesKey("countdown_time")
        val PASSCODE = stringPreferencesKey("passcode")
        
        // Emergency Info Keys
        val EM_NAME = stringPreferencesKey("em_name")
        val EM_BLOOD_TYPE = stringPreferencesKey("em_blood_type")
        val EM_ALLERGIES = stringPreferencesKey("em_allergies")
        val EM_CONDITIONS = stringPreferencesKey("em_conditions")
        val EM_CONTACT_NAME = stringPreferencesKey("em_contact_name")
        val EM_CONTACT_PHONE = stringPreferencesKey("em_contact_phone")
    }

    // Flow to read all settings
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map {
        val countdownTime = it[PreferenceKeys.COUNTDOWN_TIME] ?: 30
        val passcode = it[PreferenceKeys.PASSCODE] ?: "1234"
        
        val emergencyInfo = EmergencyInfo(
            name = it[PreferenceKeys.EM_NAME] ?: "",
            bloodType = it[PreferenceKeys.EM_BLOOD_TYPE] ?: "",
            allergies = it[PreferenceKeys.EM_ALLERGIES] ?: "",
            conditions = it[PreferenceKeys.EM_CONDITIONS] ?: "",
            contactName = it[PreferenceKeys.EM_CONTACT_NAME] ?: "",
            contactPhone = it[PreferenceKeys.EM_CONTACT_PHONE] ?: ""
        )
        
        UserSettings(countdownTime, passcode, emergencyInfo)
    }

    suspend fun saveCountdownTime(time: Int) {
        context.dataStore.edit {
            it[PreferenceKeys.COUNTDOWN_TIME] = time
        }
    }

    suspend fun savePasscode(passcode: String) {
        context.dataStore.edit {
            it[PreferenceKeys.PASSCODE] = passcode
        }
    }
    
    suspend fun saveEmergencyInfo(info: EmergencyInfo) {
        context.dataStore.edit {
            it[PreferenceKeys.EM_NAME] = info.name
            it[PreferenceKeys.EM_BLOOD_TYPE] = info.bloodType
            it[PreferenceKeys.EM_ALLERGIES] = info.allergies
            it[PreferenceKeys.EM_CONDITIONS] = info.conditions
            it[PreferenceKeys.EM_CONTACT_NAME] = info.contactName
            it[PreferenceKeys.EM_CONTACT_PHONE] = info.contactPhone
        }
    }
}

data class UserSettings(
    val countdownTime: Int, 
    val passcode: String,
    val emergencyInfo: EmergencyInfo = EmergencyInfo()
)

data class EmergencyInfo(
    val name: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val contactName: String = "",
    val contactPhone: String = ""
)
