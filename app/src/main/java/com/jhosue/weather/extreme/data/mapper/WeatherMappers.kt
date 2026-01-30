package com.jhosue.weather.extreme.data.mapper

import com.jhosue.weather.extreme.data.local.entity.WeatherEntity

import com.jhosue.weather.extreme.domain.model.WeatherInfo
import com.jhosue.weather.extreme.data.remote.dto.WeatherApiResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun WeatherEntity.toWeatherInfo(): WeatherInfo {
    return WeatherInfo(
        getLatitude = latitude,
        getLongitude = longitude,
        locationName = locationName,
        temperature = currentTemperature,
        weatherCode = weatherCode,
        precipitationProbability = precipitationProbability,
        windSpeed = windSpeed,
        humidity = humidity,
        hourlyTemperatures = hourlyTemperatures,
        hourlyTimes = hourlyTimes,
        sortOrder = sortOrder
    )
}


// Mappers for Weather Data

// Mappers for Weather Data

fun WeatherApiResponse.toWeatherEntity(
    givenLocationName: String
): WeatherEntity {
    
    val current = this.current
    val forecast = this.forecast.forecastDay.firstOrNull()?.hour ?: emptyList()
    
    // Map current data
    val currentTemp = current.tempC
    val currentWindSpeed = current.windKph
    val currentHumidity = current.humidity
    val currentPrecipProb = current.precipMm.toInt() 
    val finalPrecipProb = if (current.precipMm > 0) 100 else 0 

    val currentWeatherCode = current.condition.code

    // Map Hourly Data (24 hours)
    val temps = forecast.map { it.tempC }
    
    // Format Time: "2023-10-27 13:00" -> "13:00"
    val timeFormatter = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    } else null
    
    val times = forecast.map {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && timeFormatter != null) {
             try {
                 LocalDateTime.parse(it.time, timeFormatter).hour.toString() + ":00"
             } catch (e: Exception) {
                 it.time.split(" ").last()
             }
         } else {
             it.time.split(" ").last()
         }
    }

    return WeatherEntity(
        latitude = location.lat,
        longitude = location.lon,
        locationName = if(givenLocationName == "Unknown Location") location.name else givenLocationName, 
        currentTemperature = currentTemp,
        weatherCode = currentWeatherCode,
        precipitationProbability = finalPrecipProb,
        humidity = currentHumidity,
        windSpeed = currentWindSpeed,
        hourlyTemperatures = temps,
        hourlyTimes = times
    )
}
