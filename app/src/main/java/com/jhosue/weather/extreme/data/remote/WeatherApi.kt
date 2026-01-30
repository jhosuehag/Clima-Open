package com.jhosue.weather.extreme.data.remote

import com.jhosue.weather.extreme.data.remote.dto.AzureCurrentResponse
import com.jhosue.weather.extreme.data.remote.dto.AzureForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/"
    }

    @GET("forecast")
    suspend fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): com.jhosue.weather.extreme.data.remote.dto.OpenMeteoResponseDto
    
    @GET("forecast")
    suspend fun getDailyForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m", // Need current for header
        @Query("hourly") hourly: String = "temperature_2m,weather_code" // Need hourly for consistency
    ): com.jhosue.weather.extreme.data.remote.dto.OpenMeteoResponseDto
    
    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "es",
        @Query("format") format: String = "json"
    ): com.jhosue.weather.extreme.data.remote.dto.GeocodingResponseDto
}
