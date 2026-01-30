package com.jhosue.weather.extreme.presentation.components

import com.jhosue.weather.extreme.R

object WeatherUtils {
    fun getIconResourceForWeatherCode(code: Int, isDay: Boolean): Int {
        return when (code) {
            0 -> if (isDay) R.drawable.ic_sunny else R.drawable.ic_moon
            1, 2, 3 -> R.drawable.ic_partly_cloudy // Or distinguish partly vs cloudy
            45, 48 -> R.drawable.ic_fog
            51, 53, 55, 56, 57 -> R.drawable.ic_rain // Drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_rain
            71, 73, 75, 77, 85, 86 -> R.drawable.ic_snow
            95, 96, 99 -> R.drawable.ic_storm
            else -> if (isDay) R.drawable.ic_sunny else R.drawable.ic_moon
        }
    }

    fun getBackgroundBrush(code: Int, isDay: Boolean): androidx.compose.ui.graphics.Brush {
        if (!isDay) {
            // Night: Dark Blue to Black
            return androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color(0xFF1A237E),
                    androidx.compose.ui.graphics.Color.Black
                )
            )
        }
        
        return when (code) {
            0 -> {
                // Clear Day: Orange to Light Orange
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFFFF9800),
                        androidx.compose.ui.graphics.Color(0xFFFFB74D)
                    )
                )
            }
            1, 2, 3, 45, 48 -> {
                // Cloudy: Grey to Light Grey
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF757575),
                        androidx.compose.ui.graphics.Color(0xFFBDBDBD)
                    )
                )
            }
            in 51..99 -> {
                // Rain: Dark Grey to Blue Grey
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF37474F),
                        androidx.compose.ui.graphics.Color(0xFF607D8B)
                    )
                )
            }
            else -> {
                // Default Day
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFFFF9800),
                        androidx.compose.ui.graphics.Color(0xFFFFB74D)
                    )
                )
            }
        }
    }
}
