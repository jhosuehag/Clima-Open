package com.jhosue.weather.extreme.data.repository

import com.jhosue.weather.extreme.core.LocationConstants
import com.jhosue.weather.extreme.core.Resource
import com.jhosue.weather.extreme.data.local.dao.WeatherDao
import com.jhosue.weather.extreme.data.mapper.toWeatherInfo
import com.jhosue.weather.extreme.data.mapper.toWeatherEntity
import com.jhosue.weather.extreme.data.remote.WeatherApi
import com.jhosue.weather.extreme.domain.model.WeatherInfo
import com.jhosue.weather.extreme.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.abs

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val dao: WeatherDao,
    private val context: android.content.Context
) : WeatherRepository {

    override suspend fun getWeatherData(lat: Double, long: Double, fetchRemote: Boolean, addToHistory: Boolean): Flow<Resource<WeatherInfo>> {
        return flow {
            emit(Resource.Loading())

            val localWeather = dao.getWeather(lat, long).firstOrNull()
            emit(Resource.Loading(data = localWeather?.toWeatherInfo()))

            // If we have local data and don't want to fetch, we stop here basically.
            // But we should emit Success if we have data.
            if (!fetchRemote && localWeather != null) {
                emit(Resource.Success(data = localWeather.toWeatherInfo()))
                return@flow
            }
            
            if(!fetchRemote && localWeather == null) {
                emit(Resource.Error("Sin datos locales y actualización desactivada."))
                return@flow
            }

            try {
                // Determine location name
                val locationName = localWeather?.locationName ?: when {
                    isClose(lat, long, LocationConstants.METRO_LA_HACIENDA_LAT, LocationConstants.METRO_LA_HACIENDA_LON) -> "Metro La Hacienda"
                    isClose(lat, long, LocationConstants.OVALO_LA_PERLA_LAT, LocationConstants.OVALO_LA_PERLA_LON) -> "Ovalo La Perla"
                    isClose(lat, long, LocationConstants.ESTACION_LA_CULTURA_LAT, LocationConstants.ESTACION_LA_CULTURA_LON) -> "Estacion La Cultura"
                    else -> "Ubicación Desconocida"
                }

                // Open-Meteo Fetch
                val response = try {
                    api.getWeatherData(lat = lat, long = long)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                
                if (response == null) {
                    throw Exception("No se pudo obtener datos de Open-Meteo.")
                }
                
                // Map to Entity (Full Data) with PRESERVED SortOrder
                val existingSortOrder = localWeather?.sortOrder ?: 0
                val entity = mapOpenMeteoToEntity(response, lat, long, locationName, existingSortOrder)
                
                if (addToHistory) {
                    dao.insertWeather(entity)
                }
                
                // Trigger Widget Update

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                         com.jhosue.weather.extreme.presentation.widget.WeatherGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                emit(Resource.Success(data = entity.toWeatherInfo()))
                
            } catch(e: HttpException) {
                emit(Resource.Error(
                    message = "Error de API: ${e.code()}",
                    data = localWeather?.toWeatherInfo()
                ))
            } catch(e: IOException) {
                emit(Resource.Error(
                    message = "Sin conexión a internet.",
                    data = localWeather?.toWeatherInfo()
                ))
            } catch(e: Exception) {
                 e.printStackTrace()
                 emit(Resource.Error(
                    message = "Error: ${e.message}",
                    data = localWeather?.toWeatherInfo()
                ))
            }
        }
    }
    
    // Open-Meteo Mapper
    private fun mapOpenMeteoToEntity(
        response: com.jhosue.weather.extreme.data.remote.dto.OpenMeteoResponseDto,
        lat: Double,
        lon: Double,
        name: String,
        sortOrder: Int = 0 // Added parameter
    ): com.jhosue.weather.extreme.data.local.entity.WeatherEntity {
        
        // Open-Meteo uses WMO codes directly, similar to what we map to.
        // We can just use them or simple map roughly.
        // Our domain codes: 
        // 1000 Clear, 1003 Partly Cloudy, 1183 Rain, 1276 Thunder, 1213 Snow
        val wmo = response.current.weatherCode
        val weatherCode = when(wmo) {
            0, 1 -> 1000 // Clear
            2, 3 -> 1003 // Cloudy
            45, 48 -> 1003 // Fog -> Cloudy
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> 1183 // Rain
            71, 73, 75, 77, 85, 86 -> 1213 // Snow
            95, 96, 99 -> 1276 // Thunderstorm
            else -> 1003
        }
        
        // Hourly Data (Next 24 chunks usually)
        val hourlyTimes = response.hourly.time.take(24).map { 
             try { it.substringAfter("T") } catch(e: Exception) { "" }
        }
        val hourlyTemps = response.hourly.temperatures.take(24)

        return com.jhosue.weather.extreme.data.local.entity.WeatherEntity(
            latitude = lat,
            longitude = lon,
            locationName = name,
            currentTemperature = response.current.temperature,
            weatherCode = weatherCode,
            precipitationProbability = 0, // Open-Meteo FREE doesn't give precip prob easily in simple call, leaving 0 for now or user can upgrade.
            humidity = response.current.humidity,
            windSpeed = response.current.windSpeed,
            hourlyTimes = hourlyTimes,
            hourlyTemperatures = hourlyTemps,
            sortOrder = sortOrder // Use passed order
        )
    }

    override suspend fun getCachedWeather(lat: Double, long: Double): WeatherInfo? {
        return dao.getWeather(lat, long).firstOrNull()?.toWeatherInfo()
    }
    
    private fun isClose(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
        val threshold = 0.001
        return abs(lat1 - lat2) < threshold && abs(lon1  - lon2) < threshold
    }

    override suspend fun searchLocations(query: String): Resource<List<com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto>> {
        return try {
            val response = api.searchLocations(name = query)
            val mapped = response.results?.map { 
                com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto(
                    id = it.id.toInt(),
                    name = it.name,
                    region = it.admin1 ?: "",
                    country = it.countryCode ?: "",
                    lat = it.latitude,
                    lon = it.longitude,
                    url = ""
                )
            } ?: emptyList()
            Resource.Success(mapped)
        } catch(e: Exception) {
            e.printStackTrace()
             Resource.Error("Error buscando: ${e.message}")
        }
    }

    override suspend fun saveFavorite(lat: Double, lon: Double, name: String, sortOrder: Int?, fetchRemote: Boolean): Resource<Unit> {
        return try {
             // Logic to fetch remote or use existing
             
             // If fetchRemote is FALSE, we just want to update metadata (name, sortOrder) on existing Entity.
             if(!fetchRemote) {
                 val existing = dao.getWeather(lat, lon).firstOrNull()
                 if(existing != null) {
                     val finalSortOrder = sortOrder ?: existing.sortOrder
                     val updated = existing.copy(locationName = name, sortOrder = finalSortOrder)
                     dao.insertWeather(updated)
                     return Resource.Success(Unit)
                 }
                 // If no existing, strictly we cannot save without data if api is disallowed.
                 // But typically saveFavorite is called on valid items.
                 // If we must save new item without network... we can't create valid weather data.
                 // So we fallthrough to fetch if not found? Or Error?
                 // Let's attempt fetch if not found even if fetchRemote=false? No, respect flag.
                 return Resource.Error("No se encontraron datos locales para actualizar.")
             }

             // Fetch data BUT use the provided 'name' when saving to Entity
             val response = try {
                 api.getWeatherData(lat = lat, long = lon) 
             } catch (e: Exception) {
                 e.printStackTrace()
                 null
             }
             
             if (response != null) {
                 // Determine sort order
                 val finalSortOrder = if (sortOrder != null) {
                     sortOrder
                 } else {
                     val existing = dao.getWeather(lat, lon).firstOrNull()
                     existing?.sortOrder ?: 0
                 }

                 // Use mapOpenMeteoToEntity but pass our CUSTOM 'name'
                 val entity = mapOpenMeteoToEntity(response, lat, lon, name).copy(sortOrder = finalSortOrder)
                 dao.insertWeather(entity)
                 
                 // Trigger Widget Update
                 CoroutineScope(Dispatchers.IO).launch {
                    try {
                         com.jhosue.weather.extreme.presentation.widget.WeatherGlanceWidget().updateAll(context)
                    } catch (e: Exception) { e.printStackTrace() }
                 }
                 
                 Resource.Success(Unit)
             } else {
                 Resource.Error("No se pudo obtener datos para guardar.")
             }
        } catch(e: Exception) {
            Resource.Error("Error guardando favorito: ${e.message}")
        }
    }

    override suspend fun deleteFavorite(lat: Double, lon: Double) {
        dao.deleteWeather(lat, lon)
    }

    override fun getSavedLocations(): kotlinx.coroutines.flow.Flow<List<WeatherInfo>> {
        return dao.getAllWeather().map { entities ->
            entities.map { it.toWeatherInfo() }
        }
    }

    override suspend fun getDailyForecast(lat: Double, long: Double): Resource<WeatherInfo> {
        return try {
            val response = api.getDailyForecast(lat = lat, long = long)
            
            // Map Daily
            val dailyList = response.daily?.let { daily ->
                 daily.time.mapIndexed { index, time ->
                     com.jhosue.weather.extreme.domain.model.DailyWeatherInfo(
                         time = time,
                         maxTemp = daily.maxTemps.getOrElse(index) { 0.0 },
                         minTemp = daily.minTemps.getOrElse(index) { 0.0 },
                         weatherCode = daily.weatherCodes.getOrElse(index) { 0 }
                     )
                 }
            } ?: emptyList()
            
            // reuse logic via entity or manual map
            val entity = mapOpenMeteoToEntity(response, lat, long, "Temp", 0)
            val info = entity.toWeatherInfo().copy(dailyForecast = dailyList)
            
            Resource.Success(info)
        } catch(e: Exception) {
            e.printStackTrace()
            Resource.Error("Error cargando pronóstico: ${e.message}")
        }
    }
}
