package com.jhosue.weather.extreme.domain.repository

import com.jhosue.weather.extreme.core.Resource
import com.jhosue.weather.extreme.domain.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getWeatherData(lat: Double, long: Double, fetchRemote: Boolean = true, addToHistory: Boolean = true): Flow<Resource<WeatherInfo>>
    suspend fun getCachedWeather(lat: Double, long: Double): WeatherInfo?
    
    // New Methods for Favorites
    suspend fun searchLocations(query: String): Resource<List<com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto>>
    suspend fun saveFavorite(lat: Double, lon: Double, name: String, sortOrder: Int? = null, fetchRemote: Boolean = true): Resource<Unit>
    suspend fun deleteFavorite(lat: Double, lon: Double)
    fun getSavedLocations(): Flow<List<WeatherInfo>>
    suspend fun getDailyForecast(lat: Double, long: Double): Resource<WeatherInfo>
}
