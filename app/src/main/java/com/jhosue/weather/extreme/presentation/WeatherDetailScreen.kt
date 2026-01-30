package com.jhosue.weather.extreme.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jhosue.weather.extreme.presentation.components.HourlyTemperatureChart
import com.jhosue.weather.extreme.presentation.components.WeatherCard
import com.jhosue.weather.extreme.presentation.model.WeatherType

@Composable
fun WeatherDetailScreen(
    locationName: String?,
    lat: Double?,
    lng: Double?,
    state: WeatherState,
    navController: NavController,
    loadDailyForecast: (Double, Double) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            loadDailyForecast(lat, lng)
        }
    }
    
    val info = state.detailWeather
    val isLoading = state.isLoading
    
    // Fallback title if passed name is null
    val displayedTitle = locationName ?: info?.locationName ?: "Detalles"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF101010)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, 
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
                Text(
                    text = displayedTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isLoading && info == null) {
                Box(modifier = Modifier.fillMaxSize().height(400.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (info != null) {
                // Reuse Weather Card for Summary
                WeatherCard(
                    locationName = "Condiciones Actuales",
                    weatherInfo = info,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pronóstico 7 Días",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Daily List
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    info.dailyForecast.forEach { day ->
                        DailyForecastItem(day)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tendencia Horaria (24h)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                HourlyTemperatureChart(
                    temps = info.hourlyTemperatures,
                    times = info.hourlyTimes
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Métricas Detalladas",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Grid of Details
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(horizontal = 16.dp),
                   horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailItem(
                        icon = Icons.Default.WaterDrop,
                        label = "Humedad",
                        value = "${info.humidity}%",
                        color = Color(0xFF4FC3F7),
                        modifier = Modifier.weight(1f)
                    )
                    DetailItem(
                        icon = Icons.Default.Air,
                        label = "Viento",
                        value = "${info.windSpeed} km/h",
                        color = Color(0xFF81C784),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                 Text(
                    text = "Seleccione una ubicación válida.",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun DailyForecastItem(day: com.jhosue.weather.extreme.domain.model.DailyWeatherInfo) {
    val dateStr = try {
        // Parse "2023-10-27" to Day Name if possible, or just show date
        java.time.LocalDate.parse(day.time).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("es", "ES")).replaceFirstChar { it.uppercase() }
    } catch(e: Exception) {
        day.time
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = dateStr, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            
            // Icon
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.jhosue.weather.extreme.presentation.components.WeatherUtils.getIconResourceForWeatherCode(day.weatherCode, true)),
                contentDescription = null,
                tint = Color.Unspecified, // Use original colors or White? Let's use White for consistency or Unspecified if drawable has colors. 
                // Our drawables in XML have colors, so Unspecified renders them as is. 
                // BUT wait, existing icons might be vectors with hardcoded colors or tints.
                // The WeatherCard used tint=White. Let's use tint=Color.White for safety or Unspecified if we want colorful icons.
                // The icons I created have android:fillColor hardcoded. So Unspecified is best.
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            Text(text = "Min: ${day.minTemp}°", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.size(16.dp))
            Text(text = "Max: ${day.maxTemp}°", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
