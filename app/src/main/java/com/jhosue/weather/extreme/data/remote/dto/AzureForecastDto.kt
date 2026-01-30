package com.jhosue.weather.extreme.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AzureForecastResponse(
    @SerializedName("forecasts") val forecasts: List<AzureHourlyForecastItem>
)

data class AzureHourlyForecastItem(
    @SerializedName("date") val date: String,
    @SerializedName("temperature") val temperature: AzureValue,
    @SerializedName("iconCode") val iconCode: Int,
    @SerializedName("phrase") val phrase: String, // Short description like "Soleado"
    @SerializedName("precipitationProbability") val precipitationProbability: Int?
)

data class AzureValue(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String
)
