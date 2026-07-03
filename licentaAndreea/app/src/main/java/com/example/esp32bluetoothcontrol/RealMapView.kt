package com.example.esp32bluetoothcontrol
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*

@Composable
fun RealMapView(
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val obstacles = sensorViewModel.obstaclePoints
    val pose by sensorViewModel.robotPose
    val isAutoMode by sensorViewModel.autoEnabled

    Canvas(
        modifier = modifier.background(Color(0xFF1A1A2E))
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f

        val minDim = minOf(w, h)
        val gridRadiusCm = 150f
        val scale = minDim / (2 * gridRadiusCm)
        val gridRadiusPx = gridRadiusCm * scale

        val robotAngleDeg = pose.theta
        val robotAngleRad = Math.toRadians(robotAngleDeg.toDouble()).toFloat()

        //============================================
        //CLIP TOT IN GRILA
        //============================================
        clipRect(
            left   = centerX - gridRadiusPx,
            top    = centerY - gridRadiusPx,
            right  = centerX + gridRadiusPx,
            bottom = centerY + gridRadiusPx
        ) {
            //Linii grila
            val stepCm = 50f
            var lineCm = -gridRadiusCm
            while (lineCm <= gridRadiusCm) {
                val linePx = lineCm * scale
                drawLine(Color(0x22FFFFFF), Offset(centerX + linePx, centerY - gridRadiusPx), Offset(centerX + linePx, centerY + gridRadiusPx), strokeWidth = 0.5f)
                drawLine(Color(0x22FFFFFF), Offset(centerX - gridRadiusPx, centerY + linePx), Offset(centerX + gridRadiusPx, centerY + linePx), strokeWidth = 0.5f)
                lineCm += stepCm
            }

            //Cercuri concentrice
            listOf(50f, 100f, 150f).forEach { distCm ->
                val r = distCm * scale
                drawCircle(Color(0x33FFFFFF), r, Offset(centerX, centerY), style = Stroke(width = 1f))
                drawCircle(Color(0x18FFFFFF), r, Offset(centerX, centerY), style = Stroke(width = 0.5f))
            }

            //Marker nord
            val northY = centerY - 150f * scale
            drawLine(Color.Red.copy(alpha = 0.8f), Offset(centerX - 12f, northY - 20f), Offset(centerX - 12f, northY + 20f), strokeWidth = 3f)
            drawLine(Color.Red.copy(alpha = 0.8f), Offset(centerX - 12f, northY - 20f), Offset(centerX + 12f, northY + 20f), strokeWidth = 3f)
            drawLine(Color.Red.copy(alpha = 0.8f), Offset(centerX + 12f, northY - 20f), Offset(centerX + 12f, northY + 20f), strokeWidth = 3f)

            //============================
            //OBSTACOLE - coordonate globale, fara rotatie
            //============================
            obstacles.forEach { p ->
                val dx = p.x - pose.x
                val dy = p.y - pose.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                if (dist > 1f && dist <= gridRadiusCm) {
                    val screenX = centerX + dx * scale
                    val screenY = centerY - dy * scale

                    val color = when {
                        dist < 30  -> Color.Red
                        dist < 80  -> Color(0xFFFF9100)
                        dist < 150 -> Color.Yellow
                        else       -> Color(0xAAFFFF00)
                    }

                    drawCircle(color, 8f, Offset(screenX, screenY))
                    drawCircle(Color.White, 8f, Offset(screenX, screenY), style = Stroke(width = 2f))
                }
            }
            //============================
            //ROBOT
            //============================
            drawCircle(Color(0x44FFFFFF), 28f, Offset(centerX, centerY))
            drawCircle(Color(0xFF00BCD4), 18f, Offset(centerX, centerY))
            drawCircle(Color(0xFF00838F), 12f, Offset(centerX, centerY))

            val arrowLen = 35f
            val tipX = centerX + arrowLen * sin(robotAngleRad)
            val tipY = centerY - arrowLen * cos(robotAngleRad)
            drawLine(Color(0xFF00E5FF), Offset(centerX, centerY), Offset(tipX, tipY), strokeWidth = 5f)

            val arrowBaseX = centerX + (arrowLen - 12f) * sin(robotAngleRad)
            val arrowBaseY = centerY - (arrowLen - 12f) * cos(robotAngleRad)
            val perpX = cos(robotAngleRad)
            val perpY = sin(robotAngleRad)
            val arrowPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(arrowBaseX - 9f * perpX, arrowBaseY - 9f * perpY)
                lineTo(arrowBaseX + 9f * perpX, arrowBaseY + 9f * perpY)
                close()
            }
            drawPath(arrowPath, Color(0xFF00E5FF))
            drawCircle(Color.White, 4f, Offset(centerX, centerY))
        }

        //Compas
        val compX = w - 50f
        val compY = 55f
        val compR = 30f
        drawCircle(Color(0x44FFFFFF), compR, Offset(compX, compY), style = Stroke(1.5f))
        drawLine(Color.Red, Offset(compX, compY), Offset(compX, compY - compR + 5f), strokeWidth = 3f)
        drawLine(Color(0x88FFFFFF), Offset(compX, compY), Offset(compX, compY + compR - 5f), strokeWidth = 2f)
        drawLine(Color(0x88FFFFFF), Offset(compX, compY), Offset(compX + compR - 5f, compY), strokeWidth = 2f)
        drawLine(Color(0x88FFFFFF), Offset(compX, compY), Offset(compX - compR + 5f, compY), strokeWidth = 2f)

        val compPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE; textSize = 16f; isAntiAlias = true; alpha = 200
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("N", compX, compY - compR - 8f, compPaint)
        drawContext.canvas.nativeCanvas.drawText("S", compX, compY + compR + 18f, compPaint)
        drawContext.canvas.nativeCanvas.drawText("E", compX + compR + 18f, compY + 6f, compPaint)
        drawContext.canvas.nativeCanvas.drawText("W", compX - compR - 18f, compY + 6f, compPaint)

        //Info
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE; textSize = 24f; isAntiAlias = true; alpha = 200
        }
        drawContext.canvas.nativeCanvas.drawText("θ: ${robotAngleDeg.toInt()}°", 15f, h - 20f, paint)
        paint.textSize = 20f
        paint.color = if (isAutoMode) android.graphics.Color.GREEN else android.graphics.Color.YELLOW
        drawContext.canvas.nativeCanvas.drawText(if (isAutoMode) "AUTO" else "MANUAL", 15f, h - 45f, paint)
        paint.textSize = 16f
        paint.color = android.graphics.Color.argb(150, 255, 255, 255)
        drawContext.canvas.nativeCanvas.drawText("Puncte: ${obstacles.size}", 15f, h - 65f, paint)
    }
}