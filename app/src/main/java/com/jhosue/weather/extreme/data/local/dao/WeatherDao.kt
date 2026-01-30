package com.jhosue.weather.extreme.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jhosue.weather.extreme.data.local.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    // Select by composite key (lat/lon) with some tolerance if needed, 
    // but for now strict equality as per composite key definition.
    @Query("SELECT * FROM weather_table WHERE latitude = :lat AND longitude = :lon")
    fun getWeather(lat: Double, lon: Double): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_table")
    fun getAllWeather(): Flow<List<WeatherEntity>>

    @Query("DELETE FROM weather_table WHERE latitude = :lat AND longitude = :lon")
    suspend fun deleteWeather(lat: Double, lon: Double)
}
