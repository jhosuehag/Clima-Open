package com.jhosue.weather.extreme.presentation

import com.jhosue.weather.extreme.domain.model.WeatherInfo

data class WeatherState(
    val weatherInfo: Map<String, WeatherInfo?> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFahrenheit: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val currentLocationWeather: WeatherInfo? = null,
    val searchResults: List<com.jhosue.weather.extreme.data.remote.dto.SearchResponseDto> = emptyList(),
    val isSearching: Boolean = false,
    val isEditMode: Boolean = false,
    val detailWeather: WeatherInfo? = null
)
