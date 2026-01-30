package com.jhosue.weather.extreme.data.local.entity

import androidx.room.Entity
import androidx.room.TypeConverters
import com.jhosue.weather.extreme.data.local.Converters

@Entity(
    tableName = "weather_table",
    primaryKeys = ["latitude", "longitude"]
)
data class WeatherEntity(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val currentTemperature: Double,
    val weatherCode: Int,
    val precipitationProbability: Int,
    val humidity: Int,
    val windSpeed: Double,
    val hourlyTimes: List<String>,
    val hourlyTemperatures: List<Double>,
    val sortOrder: Int = 0 // New field for reordering
)
