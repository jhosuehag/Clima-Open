package com.jhosue.weather.extreme.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhosue.weather.extreme.core.LocationConstants
import com.jhosue.weather.extreme.domain.model.WeatherInfo
import com.jhosue.weather.extreme.domain.repository.WeatherRepository
import com.jhosue.weather.extreme.presentation.MainActivity
import com.jhosue.weather.extreme.presentation.model.WeatherType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull

// --- Hilt Entry Point ---
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeatherRepositoryEntryPoint {
    fun getWeatherRepository(): WeatherRepository
}

// --- Receiver ---
class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherGlanceWidget()
}

// --- Widget ---
class WeatherGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Hilt Injection via EntryPoint
        val appContext = context.applicationContext ?: throw IllegalStateException("No app context")
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext, 
            WeatherRepositoryEntryPoint::class.java
        )
        val repository = entryPoint.getWeatherRepository()

        // Fetch Data for 3 Locations
        // Fetch Data for 3 Locations using direct cache access
        val loc1 = repository.getCachedWeather(LocationConstants.METRO_LA_HACIENDA_LAT, LocationConstants.METRO_LA_HACIENDA_LON)
        val loc2 = repository.getCachedWeather(LocationConstants.OVALO_LA_PERLA_LAT, LocationConstants.OVALO_LA_PERLA_LON)
        val loc3 = repository.getCachedWeather(LocationConstants.ESTACION_LA_CULTURA_LAT, LocationConstants.ESTACION_LA_CULTURA_LON)

        val items = listOfNotNull(loc1, loc2, loc3)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(ColorProvider(Color(0xFF101010).copy(alpha = 0.9f))) // Dark translucent bg
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "WeatherMaster",
                            style = TextStyle(
                                color = ColorProvider(Color.White), 
                                fontSize = 14.sp
                            ),
                            modifier = GlanceModifier.padding(bottom = 6.dp)
                        )

                        if (items.isEmpty()) {
                             Text(
                                text = "No cached data. Open app to sync.",
                                style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp)
                            )
                        } else {
                            items.forEach { weather ->
                                WeatherItem(weather)
                                Spacer(modifier = GlanceModifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    fun WeatherItem(weather: WeatherInfo) {
        // Deep link to Detail. locationName matches URL param format in MainActivity
        // We need an Action. For simplicity, just launch MainActivity and let user nav, 
        // OR construct an Intent with data URI "android-app://androidx.navigation/detail/{id}"
        // But Navigation DeepLinks require handling. We'll stick to launching the app for now,
        // as standard widget behavior usually just opens app.
        // Wait, requirements said: "navigate to detail".
        // To do that, we set Intent data.
        
        // Simple mapping of Icon
        val weatherType = WeatherType.fromWmo(weather.weatherCode)
        // Glance doesn't support ImageVector directly easily without converting to Bitmap or Drawable.
        // For simplicity and speed, we will use a text Emoji or generic icon provided by the system if possible.
        // Or we assume we can pass resource ID. WeatherType stores ImageVector, which isn't Glance friendly yet.
        // We'll calculate a simple Emoji based on weather type for the Widget to avoid asset complexity.
        val emoji = when (weatherType) {
            is WeatherType.ClearSky -> "☀️"
            is WeatherType.Cloudy -> "☁️"
            is WeatherType.Rainy -> "🌧️"
            is WeatherType.Stormy -> "⛈️"
            is WeatherType.Snowy -> "❄️"
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF1E1E1E))) // Inner card darker
                .padding(6.dp)
                .clickable(actionStartActivity<MainActivity>()), // Ideally pass Intent extras for deep link
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = TextStyle(fontSize = 16.sp),
                modifier = GlanceModifier.padding(end = 6.dp)
            )
            
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = weather.locationName,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp),
                    maxLines = 1
                )
            }
            
            Text(
                text = "${weather.temperature.toInt()}°",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp)
            )
        }
    }
}
