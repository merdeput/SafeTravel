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
    }

    // Flow to read all settings
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map {
        val countdownTime = it[PreferenceKeys.COUNTDOWN_TIME] ?: 30
        val passcode = it[PreferenceKeys.PASSCODE] ?: "1234"
        UserSettings(countdownTime, passcode)
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
}

data class UserSettings(val countdownTime: Int, val passcode: String)
