package com.jhosue.weather.extreme.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jhosue.weather.extreme.R
import com.jhosue.weather.extreme.presentation.MainActivity
import com.jhosue.weather.extreme.presentation.components.WeatherUtils

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "weather_prefs"
        private const val KEY_LOCATION = "last_location"
        private const val KEY_TEMP = "last_temp"
        private const val KEY_DESC = "last_desc"
        private const val KEY_ICON_CODE = "last_icon_code"
        private const val KEY_IS_DAY = "last_is_day"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 1. Get SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val locationName = prefs.getString(KEY_LOCATION, "Sin datos")
            val temp = prefs.getFloat(KEY_TEMP, 0f)
            val desc = prefs.getString(KEY_DESC, "Toque para actualizar")
            val iconCode = prefs.getInt(KEY_ICON_CODE, 0)
            val isDay = prefs.getBoolean(KEY_IS_DAY, true)

            // 2. Setup Layout
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            views.setTextViewText(R.id.widget_location_name, locationName)
            views.setTextViewText(R.id.widget_temperature, "${temp.toInt()}°")
            views.setTextViewText(R.id.widget_description, desc)
            
            // Icon Logic
            val iconRes = WeatherUtils.getIconResourceForWeatherCode(iconCode, isDay)
            views.setImageViewResource(R.id.widget_icon, iconRes)

            // 3. Pending Intent to Open App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 4. Update
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
