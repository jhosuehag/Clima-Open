package com.jhosue.weather.extreme.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhosue.weather.extreme.core.LocationConstants
import com.jhosue.weather.extreme.core.Resource
import com.jhosue.weather.extreme.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.jhosue.weather.extreme.domain.location.LocationTracker
import com.jhosue.weather.extreme.presentation.widget.WidgetWeatherItem
import com.google.gson.Gson
import android.appwidget.AppWidgetManager
import com.jhosue.weather.extreme.presentation.widget.WeatherWidget
import com.jhosue.weather.extreme.R
import android.content.Context
import android.content.Intent


@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationTracker: LocationTracker,
    private val userPreferencesRepository: com.jhosue.weather.extreme.data.local.UserPreferencesRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = mutableStateOf(WeatherState())
    val state: State<WeatherState> = _state

    init {
        // Observe stored locations and preferences
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _state.value = _state.value.copy(
                    isFahrenheit = prefs.isFahrenheit,
                    isNotificationsEnabled = prefs.notificationsEnabled
                )
                
                // 1. First Run Logic
                if (prefs.isFirstRun) {
                    preloadDefaults()
                    userPreferencesRepository.setFirstRun(false)
                }
            }
        }
        
        loadSavedLocations()
        // Load only local cache on startup
        loadCurrentLocationWeather(fetchRemote = false)
    }
    
    // New: Load from Room DB
    private fun loadSavedLocations() {
        viewModelScope.launch {
            repository.getSavedLocations().collect { list ->
                // CORRECTION: LIST EMPTY LOGIC
                // Do NOT automatically load defaults if empty (unless first run handled above).
                // Just show empty list.
                val map = list.associate { it.locationName to it }
                _state.value = _state.value.copy(
                    weatherInfo = map
                )
                updateWidget()
            }
        }
    }
    
    // 2. Restore Defaults Feature
    fun restoreDefaultLocations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // 1. CLEAR: Delete all existing locations
            val currentList = repository.getSavedLocations().firstOrNull() ?: emptyList()
            currentList.forEach { info ->
                repository.deleteFavorite(info.getLatitude, info.getLongitude)
            }
            
            // 2. REFILL: Add GPS (if available) + 3 Fixed Locations
            val newItems = mutableListOf<Triple<String, Double, Double>>()
            
            // Item 1: GPS "Ubicación Actual"
            val loc = locationTracker.getCurrentLocation()
            if (loc != null) {
                newItems.add(Triple("Ubicación Actual", loc.latitude, loc.longitude))
            }
            
            // Fixed Constant Items
            newItems.add(Triple("Ovalo La Perla", LocationConstants.OVALO_LA_PERLA_LAT, LocationConstants.OVALO_LA_PERLA_LON))
            newItems.add(Triple("Metro La Hacienda", LocationConstants.METRO_LA_HACIENDA_LAT, LocationConstants.METRO_LA_HACIENDA_LON))
            newItems.add(Triple("Estacion La Cultura", LocationConstants.ESTACION_LA_CULTURA_LAT, LocationConstants.ESTACION_LA_CULTURA_LON))
            
            newItems.forEachIndexed { index, (name, lat, lon) ->
                repository.saveFavorite(lat, lon, name, sortOrder = index, fetchRemote = true)
            }
             
            _state.value = _state.value.copy(isLoading = false, error = null)
            
            // Force valid refresh of new items
            refreshAll()
        }
    }

    private fun preloadDefaults() {
        viewModelScope.launch {
            val newItems = mutableListOf<Triple<String, Double, Double>>()
            
            // Item 1: GPS "Ubicación Actual"
            val loc = locationTracker.getCurrentLocation()
            if (loc != null) {
                newItems.add(Triple("Ubicación Actual", loc.latitude, loc.longitude))
            }
            
            // Fixed Constant Items
            newItems.add(Triple("Ovalo La Perla", LocationConstants.OVALO_LA_PERLA_LAT, LocationConstants.OVALO_LA_PERLA_LON))
            newItems.add(Triple("Metro La Hacienda", LocationConstants.METRO_LA_HACIENDA_LAT, LocationConstants.METRO_LA_HACIENDA_LON))
            newItems.add(Triple("Estacion La Cultura", LocationConstants.ESTACION_LA_CULTURA_LAT, LocationConstants.ESTACION_LA_CULTURA_LON))
            
            newItems.forEachIndexed { index, (name, lat, lon) ->
                repository.saveFavorite(lat, lon, name, sortOrder = index, fetchRemote = true)
            }
        }
    }

    fun loadCurrentLocationWeather(fetchRemote: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val location = locationTracker.getCurrentLocation()
            if(location != null) {
                // addToHistory = false to PREVENT adding "Current Location" to the "Saved List"
                repository.getWeatherData(
                    lat = location.latitude, 
                    long = location.longitude, 
                    fetchRemote = fetchRemote, 
                    addToHistory = false
                ).collect { result ->
                     when(result) {
                         is Resource.Success -> {
                             _state.value = _state.value.copy(
                                 currentLocationWeather = result.data,
                                 isLoading = false
                             )
                             updateWidget()
                         }
                         is Resource.Error -> {
                             _state.value = _state.value.copy(
                                 error = if(fetchRemote) "Error: ${result.message}" else null,
                                 currentLocationWeather = result.data, 
                                 isLoading = false
                             )
                         }
                         is Resource.Loading -> { 
                             if(result.data != null) {
                                 _state.value = _state.value.copy(
                                     currentLocationWeather = result.data
                                 )
                             }
                         }
                     }
                }
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
    
    // Search Logic
    fun searchLocations(query: String) {
        if(query.isBlank()) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            when(val result = repository.searchLocations(query)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        searchResults = result.data ?: emptyList(),
                        isSearching = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isSearching = false
                    )
                }
                else -> {}
            }
        }
    }

    fun addFavorite(item: com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto) {
        viewModelScope.launch {
            repository.saveFavorite(item.lat, item.lon, item.name, fetchRemote = true)
        }
    }

    fun toggleEditMode() {
        _state.value = _state.value.copy(isEditMode = !state.value.isEditMode)
    }

    fun reloadData() {
        refreshAll()
    }        
    fun refreshAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // 1. Update Header (Current Location)
            // Does NOT save to DB, keeping the list clean.
            loadCurrentLocationWeather(fetchRemote = true)
            
            // 2. Refresh Saved Locations
            // Read ONLY existing items from DB (Source of Truth)
            // Never adds new items.
            val currentList = repository.getSavedLocations().firstOrNull() ?: emptyList()
            
            currentList.forEach { info ->
                launch { 
                    // This updates the DB entity in place because addToHistory defaults to true
                    repository.getWeatherData(info.getLatitude, info.getLongitude, fetchRemote = true).collect()
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    
    fun deleteLocation(info: com.jhosue.weather.extreme.domain.model.WeatherInfo) {
        viewModelScope.launch {
            repository.deleteFavorite(info.getLatitude, info.getLongitude)
        }
    }
    
    fun renameLocation(info: com.jhosue.weather.extreme.domain.model.WeatherInfo, newName: String) {
        viewModelScope.launch {
            // Update name LOCALLY only
            repository.saveFavorite(info.getLatitude, info.getLongitude, newName, fetchRemote = false)
        }
    }
    
    // Sort logic
    fun updateSortOrder(newList: List<com.jhosue.weather.extreme.domain.model.WeatherInfo>) {
        viewModelScope.launch {
            newList.forEachIndexed { index, item ->
                 // Update sort order LOCALLY only. Fixes drag lag.
                 repository.saveFavorite(item.getLatitude, item.getLongitude, item.locationName, sortOrder = index, fetchRemote = false)
            }
        }
    }
    
    fun moveLocationUp(info: com.jhosue.weather.extreme.domain.model.WeatherInfo) {
        viewModelScope.launch {
            val currentList = state.value.weatherInfo.values.filterNotNull().sortedBy { it.sortOrder }
            val index = currentList.indexOfFirst { it.getLatitude == info.getLatitude && it.getLongitude == info.getLongitude }
            if (index > 0) {
                val mutableList = currentList.toMutableList()
                java.util.Collections.swap(mutableList, index, index - 1)
                mutableList.forEachIndexed { i, item ->
                     repository.saveFavorite(item.getLatitude, item.getLongitude, item.locationName, sortOrder = i, fetchRemote = false)
                }
            }
        }
    }
    
    fun moveLocationDown(info: com.jhosue.weather.extreme.domain.model.WeatherInfo) {
        viewModelScope.launch {
            val currentList = state.value.weatherInfo.values.filterNotNull().sortedBy { it.sortOrder }
            val index = currentList.indexOfFirst { it.getLatitude == info.getLatitude && it.getLongitude == info.getLongitude }
            if (index >= 0 && index < currentList.size - 1) {
                val mutableList = currentList.toMutableList()
                java.util.Collections.swap(mutableList, index, index + 1)
                mutableList.forEachIndexed { i, item ->
                     repository.saveFavorite(item.getLatitude, item.getLongitude, item.locationName, sortOrder = i, fetchRemote = false)
                }
            }
        }
    }

    fun toggleFahrenheit(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFahrenheit(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun triggerTestNotification() {
        val request = androidx.work.OneTimeWorkRequestBuilder<com.jhosue.weather.extreme.data.worker.SyncWeatherWorker>()
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        androidx.work.WorkManager.getInstance(context).enqueue(request)
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            // Update Header (always good)
            loadCurrentLocationWeather(fetchRemote = true)
            
            val currentList = repository.getSavedLocations().firstOrNull() ?: emptyList()
            val hasCurrentParams = currentList.any { it.locationName == "Ubicación Actual" }
            
            if (!hasCurrentParams) {
                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    // Shift existing items down to make space at index 0
                    currentList.forEach { item ->
                        repository.saveFavorite(
                            item.getLatitude, 
                            item.getLongitude, 
                            item.locationName, 
                            sortOrder = item.sortOrder + 1, 
                            fetchRemote = false
                        )
                    }
                    
                    // Add new GPS item at index 0
                    repository.saveFavorite(
                        loc.latitude, 
                        loc.longitude, 
                        "Ubicación Actual", 
                        sortOrder = 0, 
                        fetchRemote = true
                    )
                }
            }
        }
    }

    fun loadDailyForecast(lat: Double, lng: Double) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, detailWeather = null)
            when(val result = repository.getDailyForecast(lat, lng)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        detailWeather = result.data,
                        isLoading = false
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is Resource.Loading -> {
                     _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                // 1. Collect Data
                val current = state.value.currentLocationWeather
                val saved = state.value.weatherInfo.values.filterNotNull().sortedBy { it.sortOrder }
                
                val widgetItems = mutableListOf<WidgetWeatherItem>()
                
                // Add Current Location first if exists
                if (current != null) {
                    widgetItems.add(
                        WidgetWeatherItem(
                            locationName = "Ubicación Actual", // Force name or use current.locationName
                            temperature = current.temperature,
                            weatherCode = current.weatherCode,
                            isDay = current.isDay,
                            description = getWeatherDescription(current.weatherCode)
                        )
                    )
                }
                
                // Add Saved Locations
                saved.forEach { item ->
                    // Prevent duplicate "Actual" if saved list has it (it shouldn't if filtered right, but safety check)
                    if (item.locationName != "Ubicación Actual") {
                         widgetItems.add(
                            WidgetWeatherItem(
                                locationName = item.locationName,
                                temperature = item.temperature,
                                weatherCode = item.weatherCode,
                                isDay = item.isDay,
                                description = getWeatherDescription(item.weatherCode)
                            )
                        )
                    }
                }

                // 2. Serialize
                val gson = Gson()
                val json = gson.toJson(widgetItems)

                // 3. Save to Prefs
                val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("all_locations_data", json).apply()

                // 4. Notify Widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, WeatherWidget::class.java)
                )
                
                // Notify list data changed
                appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
                
                // Trigger general update to refresh title/empty view if needed
                // Valid only if we update normal views too, but focusing on list now.
                // Sending broadcast or calling onUpdate manually might be needed for non-list views.
                val intent = Intent(context, WeatherWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when(code) {
             0 -> "Despejado"
             1, 2, 3 -> "Nublado"
             45, 48 -> "Niebla"
             51, 53, 55 -> "Llovizna"
             61, 63, 65 -> "Lluvia"
             71, 73, 75 -> "Nieve"
             95, 96, 99 -> "Tormenta"
             else -> "Normal"
        }
    }
}