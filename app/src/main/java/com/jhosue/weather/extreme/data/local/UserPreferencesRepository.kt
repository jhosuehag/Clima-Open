package com.jhosue.weather.extreme.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

data class UserPreferences(
    val isFahrenheit: Boolean,
    val notificationsEnabled: Boolean,
    val isFirstRun: Boolean
)

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object PreferencesKeys {
        val IS_FAHRENHEIT = booleanPreferencesKey("is_fahrenheit")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isFahrenheit = preferences[PreferencesKeys.IS_FAHRENHEIT] ?: false
            val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            val isFirstRun = preferences[PreferencesKeys.IS_FIRST_RUN] ?: true
            UserPreferences(isFahrenheit, notificationsEnabled, isFirstRun)
        }

    suspend fun setFahrenheit(isFahrenheit: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FAHRENHEIT] = isFahrenheit
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setFirstRun(isFirstRun: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] = isFirstRun
        }
    }
}
