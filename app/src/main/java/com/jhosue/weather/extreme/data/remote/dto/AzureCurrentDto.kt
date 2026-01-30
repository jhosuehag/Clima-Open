package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AzureCurrentResponse(
    @SerializedName("results") val results: List<AzureCurrentResult>
)

data class AzureCurrentResult(
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("phrase") val phrase: String,
    @SerializedName("iconCode") val iconCode: Int,
    @SerializedName("temperature") val temperature: AzureTemperature,
    @SerializedName("relativeHumidity") val relativeHumidity: Int,
    @SerializedName("wind") val wind: AzureWind
)

data class AzureTemperature(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String
)

data class AzureWind(
    @SerializedName("speed") val speed: AzureWindSpeed
)

data class AzureWindSpeed(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String
)
