package com.jhosue.weather.extreme.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jhosue.weather.extreme.R
import com.jhosue.weather.extreme.core.LocationConstants
import com.jhosue.weather.extreme.core.Resource
import com.jhosue.weather.extreme.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class SyncWeatherWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: WeatherRepository,
    private val userPreferencesRepository: com.jhosue.weather.extreme.data.local.UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // 1. SIMPLIFICATION: Assume Notifications Enabled to rule out DataStore issues
             val notificationsEnabled = true
            
            val locations = listOf(
                Pair("La Hacienda", LocationConstants.METRO_LA_HACIENDA_LAT to LocationConstants.METRO_LA_HACIENDA_LON),
                Pair("La Perla", LocationConstants.OVALO_LA_PERLA_LAT to LocationConstants.OVALO_LA_PERLA_LON),
                Pair("La Cultura", LocationConstants.ESTACION_LA_CULTURA_LAT to LocationConstants.ESTACION_LA_CULTURA_LON)
            )

            val jobs = locations.map { (name, coords) ->
                async {
                    val (lat, lon) = coords
                    // Use collecting flow
                    val result = repository.getWeatherData(lat, lon).firstOrNull { it is Resource.Success || it is Resource.Error }

                    if (result is Resource.Success) {
                        result.data?.let { weather ->
                            // ALERTA AUTOMATICA: Temperatura mayor a 19.0 (Requisito de Usuario)
                            if (notificationsEnabled && weather.temperature > 19.0) {
                                Log.d("SyncWeatherWorker", "High Temp Alert for ${name}: ${weather.temperature}")
                                showHighTempNotification(name, weather.temperature)
                            }
                        }
                    }
                }
            }
            
            jobs.awaitAll()
            Result.success()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // Required for Expedited Work
    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notificationId = 9999
        val channelId = "weather_channel_critical_v4" // RENAMED TO FORCE RESET
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Sync Service", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Sincronizando clima")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

        return androidx.work.ForegroundInfo(notificationId, notification)
    }

    private fun showHighTempNotification(locationName: String, temperature: Double) {
        val channelId = "weather_channel_critical_v4" // RENAMED TO FORCE RESET
        
        // Defensive Channel Creation with Safe Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas Críticas"
            val descriptionText = "Notificaciones urgentes de clima"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                // Explicitly set default sound
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SyncWeatherWorker", "Permission DENIED inside Worker")
            return
        }

        val builder = NotificationCompat.Builder(context, channelId)
            // Using a standard warning icon
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Temperatura alta 🌡️")
            .setContentText("Hace $temperature° en $locationName")
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX for Heads-up
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound + Vibration
            .setAutoCancel(true)

        // Unique ID per location to allow multiple notifications
        NotificationManagerCompat.from(context).notify(locationName.hashCode(), builder.build())
    }
}
