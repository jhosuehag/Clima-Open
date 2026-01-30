package com.jhosue.weather.extreme.di

import android.app.Application
import androidx.room.Room
import com.jhosue.weather.extreme.data.local.WeatherDatabase
import com.jhosue.weather.extreme.data.remote.WeatherApi
import com.jhosue.weather.extreme.data.repository.WeatherRepositoryImpl
import com.jhosue.weather.extreme.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWeatherApi(): WeatherApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(WeatherApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherDatabase(app: Application): WeatherDatabase {
        return Room.databaseBuilder(
            app,
            WeatherDatabase::class.java,
            "weather_db"
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    @Singleton
    fun provideDataStore(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("user_preferences") }
        )
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        api: WeatherApi,
        db: WeatherDatabase,
        app: Application
    ): WeatherRepository {
        return WeatherRepositoryImpl(api, db.weatherDao, app)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(app: Application): com.google.android.gms.location.FusedLocationProviderClient {
        return com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(app)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        locationClient: com.google.android.gms.location.FusedLocationProviderClient,
        app: Application
    ): com.jhosue.weather.extreme.domain.location.LocationTracker {
        return com.jhosue.weather.extreme.data.location.DefaultLocationTracker(locationClient, app)
    }
}
