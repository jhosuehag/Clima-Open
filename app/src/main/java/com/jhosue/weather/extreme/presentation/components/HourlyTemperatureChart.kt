package com.jhosue.weather.extreme.presentation.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun HourlyTemperatureChart(
    temps: List<Double>,
    times: List<String>,
    modifier: Modifier = Modifier
) {
    if (temps.isEmpty()) return

    val zipData = temps.zip(times)
    // Simplify times to just hour (e.g. "2024-05-20T14:00" -> "14:00")
    // Assuming ISO 8601 format roughly
    val hourLabels = remember(times) {
        times.map { 
            try {
                it.substringAfter("T").take(5)
            } catch (e: Exception) {
                it
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (temps.size - 1)
        
        // Calculate min/max for scaling y-axis
        val minTemp = temps.minOrNull() ?: 0.0
        val maxTemp = temps.maxOrNull() ?: 100.0
        val tempRange = (maxTemp - minTemp).takeIf { it > 0 } ?: 1.0

        // Helper to map temp to Y coordinate (flip Y because canvas 0,0 is top-left)
        // Leave some padding top/bottom
        val verticalPadding = 50f
        fun tempToY(t: Double): Float {
            val normalized = (t - minTemp) / tempRange
            // inverse normalized because Y grows downwards
            // scale to usable height
            return height - verticalPadding - (normalized * (height - 2 * verticalPadding)).toFloat()
        }

        val path = Path()
        
        // Points
        val points = mutableListOf<Offset>()
        temps.forEachIndexed { index, temp ->
            val x = index * spacing
            val y = tempToY(temp)
            points.add(Offset(x, y))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        // Draw Gradient Area below line
        val fillPath = android.graphics.Path(path.asAndroidPath())
            .asComposePath()
            .apply {
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF64B5F6).copy(alpha = 0.5f),
                    Color.Transparent
                )
            )
        )

        // Draw Line
        drawPath(
            path = path,
            color = Color(0xFF29B6F6),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Points and Text
        val textPaint = Paint().apply {
            textSize = 12.sp.toPx()
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        
        // Show simplified labels (e.g., every 3rd or 4th item to avoid clutter)
        val step = if (temps.size > 12) 3 else 1

        points.forEachIndexed { index, point ->
            // Draw dot
            if (index % step == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                
                // Draw Temp Text
                drawContext.canvas.nativeCanvas.drawText(
                     "${temps[index].roundToInt()}°",
                     point.x,
                     point.y - 12.dp.toPx(),
                     textPaint
                )
                
                // Draw Time Text at bottom
                drawContext.canvas.nativeCanvas.drawText(
                     hourLabels[index],
                     point.x,
                     height,
                     textPaint
                )
            }
        }
    }
}
