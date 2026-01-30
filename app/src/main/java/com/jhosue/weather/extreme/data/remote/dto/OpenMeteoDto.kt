package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponseDto(
    @SerializedName("current") val current: OpenMeteoCurrentDto,
    @SerializedName("hourly") val hourly: OpenMeteoHourlyDto,
    @SerializedName("daily") val daily: OpenMeteoDailyDto? = null
)

data class OpenMeteoCurrentDto(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double
)

data class OpenMeteoHourlyDto(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("weather_code") val weatherCodes: List<Int>
)

data class OpenMeteoDailyDto(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName("temperature_2m_max") val maxTemps: List<Double>,
    @SerializedName("temperature_2m_min") val minTemps: List<Double>
)
