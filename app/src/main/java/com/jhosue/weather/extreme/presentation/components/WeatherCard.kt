package com.jhosue.weather.extreme.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhosue.weather.extreme.domain.model.WeatherInfo
import com.jhosue.weather.extreme.presentation.model.WeatherType
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WeatherCard(
    locationName: String,
    weatherInfo: WeatherInfo?,
    unit: String = "°C",
    modifier: Modifier = Modifier
) {
    // Determine WeatherType to style the card
    val weatherType = weatherInfo?.weatherCode?.let { WeatherType.fromWmo(it) } ?: WeatherType.ClearSky

    val bgBrush = WeatherUtils.getBackgroundBrush(
        code = weatherInfo?.weatherCode ?: 0,
        isDay = weatherInfo?.isDay ?: true
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            // Background Decorative Icon
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = WeatherUtils.getIconResourceForWeatherCode(weatherInfo?.weatherCode ?: 0, weatherInfo?.isDay ?: true)),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column {
                    Text(
                        text = locationName,
                        style = MaterialTheme.typography.titleLarge.copy(
                             fontWeight = FontWeight.SemiBold,
                             fontSize = 22.sp
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = weatherType.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Temp and Info
                if (weatherInfo != null) {
                    AnimatedContent(
                        targetState = weatherInfo.temperature,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(600)) with fadeOut(animationSpec = tween(600))
                        },
                        label = "TempAnimation"
                    ) { temp ->
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${temp.toInt()}",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 70.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Icon(
                             painter = androidx.compose.ui.res.painterResource(id = WeatherUtils.getIconResourceForWeatherCode(weatherInfo.weatherCode, weatherInfo.isDay)),
                             contentDescription = null,
                             tint = Color.White,
                             modifier = Modifier.size(24.dp)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         Text(
                             text = weatherType.weatherDesc,
                             style = MaterialTheme.typography.titleMedium,
                             color = Color.White
                         )
                         Spacer(modifier = Modifier.weight(1f))
                         Text(
                             text = "Lluvia: ${weatherInfo.precipitationProbability}%",
                             style = MaterialTheme.typography.bodyMedium,
                             color = Color.White.copy(alpha = 0.9f)
                         )
                    }
                } else {
                     Text(
                        text = "Cargando datos...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
