package com.jhosue.weather.extreme.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jhosue.weather.extreme.data.local.dao.WeatherDao
import com.jhosue.weather.extreme.data.local.entity.WeatherEntity

@Database(
    entities = [WeatherEntity::class],
    version = 2, // Incremented version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract val weatherDao: WeatherDao
}
