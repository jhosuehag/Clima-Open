package com.jhosue.weather.extreme.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

sealed class WeatherType(
    val weatherDesc: String,
    val icon: ImageVector,
    val colorBrush: Brush,
    val message: String
) {
    object ClearSky : WeatherType(
        weatherDesc = "Cielo Despejado",
        icon = Icons.Default.WbSunny,
        colorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFA726), Color(0xFFFF7043)) // Orange to Deep Orange
        ),
        message = "¡Es un día hermoso!"
    )

    object Cloudy : WeatherType(
        weatherDesc = "Nublado",
        icon = Icons.Default.Cloud,
        colorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF90CAF9), Color(0xFF616161)) // Light Blue to Grey
        ),
        message = "Clima perfecto para programar."
    )

    object Rainy : WeatherType(
        weatherDesc = "Lluvioso",
        icon = Icons.Default.WaterDrop,
        colorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF78909C), Color(0xFF37474F)) // Blue Grey to Dark
        ),
        message = "No olvides tu paraguas."
    )

    object Stormy : WeatherType(
        weatherDesc = "Tormentoso",
        icon = Icons.Default.Thunderstorm,
        colorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF5E35B1), Color(0xFF212121)) // Deep Purple to Black
        ),
        message = "¡Mantente a salvo dentro!"
    )

    object Snowy : WeatherType(
        weatherDesc = "Nevado",
        icon = Icons.Default.AcUnit,
        colorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE0F7FA), Color(0xFF81D4FA)) // Cyan colors
        ),
        message = "¡Haz un muñeco de nieve!"
    )

    companion object {
        fun fromWmo(code: Int): WeatherType {
            return when (code) {
                // Tomorrow.io Weather Codes
                1000, 1100 -> ClearSky // Clear, Mostly Clear
                1101, 1102, 1001 -> Cloudy // Partly Cloudy, Cloudy
                4000, 4001, 4200, 4201 -> Rainy // Drizzle, Rain
                8000 -> Stormy // Thunderstorm
                5000, 5100, 5101 -> Snowy // Snow
                6000, 6001, 6200, 6201 -> Rainy // Freezing Rain (treated as Rain/Cold for now)
                7000, 7101, 7102 -> Snowy // Ice Pellets
                else -> ClearSky
            }
        }
    }
}
