package com.jhosue.weather.extreme.domain.model

data class WeatherInfo(
    val getLatitude: Double,
    val getLongitude: Double,
    val locationName: String,
    val temperature: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val windSpeed: Double,
    val humidity: Int,
    val hourlyTemperatures: List<Double>,
    val hourlyTimes: List<String>,
    val isDay: Boolean = true,
    val sortOrder: Int = 0,
    val dailyForecast: List<DailyWeatherInfo> = emptyList()
)

data class DailyWeatherInfo(
    val time: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int
)
