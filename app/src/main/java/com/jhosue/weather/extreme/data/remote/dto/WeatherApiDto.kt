package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherApiResponse(
    @SerializedName("location")
    val location: LocationDto,
    @SerializedName("current")
    val current: CurrentDto,
    @SerializedName("forecast")
    val forecast: ForecastDto
)

data class LocationDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double
)

data class CurrentDto(
    @SerializedName("temp_c")
    val tempC: Double,
    @SerializedName("condition")
    val condition: ConditionDto,
    @SerializedName("wind_kph")
    val windKph: Double,
    @SerializedName("humidity")
    val humidity: Int,
    @SerializedName("precip_mm")
    val precipMm: Double,
    @SerializedName("is_day")
    val isDay: Int
)

data class ConditionDto(
    @SerializedName("text")
    val text: String,
    @SerializedName("code")
    val code: Int
)

data class ForecastDto(
    @SerializedName("forecastday")
    val forecastDay: List<ForecastDayDto>
)

data class ForecastDayDto(
    @SerializedName("date")
    val date: String,
    @SerializedName("hour")
    val hour: List<HourDto>
)

data class HourDto(
    @SerializedName("time")
    val time: String,
    @SerializedName("temp_c")
    val tempC: Double,
    @SerializedName("condition")
    val condition: ConditionDto,
    @SerializedName("chance_of_rain")
    val chanceOfRain: Int
)
