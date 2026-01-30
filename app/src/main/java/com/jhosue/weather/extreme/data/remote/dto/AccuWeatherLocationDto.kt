package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AccuWeatherLocationDto(
    @SerializedName("Key")
    val key: String,
    @SerializedName("LocalizedName")
    val localizedName: String
)
