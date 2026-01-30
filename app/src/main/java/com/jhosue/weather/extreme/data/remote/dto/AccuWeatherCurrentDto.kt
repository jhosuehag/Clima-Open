package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccuWeatherCurrentDto(
    @SerializedName("Temperature")
    val temperature: TemperatureData,
    @SerializedName("WeatherText")
    val weatherText: String,
    @SerializedName("WeatherIcon")
    val weatherIcon: Int,
    @SerializedName("HasPrecipitation")
    val hasPrecipitation: Boolean,
    @SerializedName("IsDayTime")
    val isDayTime: Boolean,
    // Add other fields if needed, but these are core
)

data class TemperatureData(
    @SerializedName("Metric")
    val metric: MetricData
)

data class MetricData(
    @SerializedName("Value")
    val value: Double,
    @SerializedName("Unit")
    val unit: String
)
