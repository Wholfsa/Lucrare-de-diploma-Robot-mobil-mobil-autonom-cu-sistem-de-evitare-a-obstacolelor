package com.example.esp32bluetoothcontrol
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarView(
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val data by sensorViewModel.sensorData

    //Animatie mai lenta pentru performanta
    val angleAnim by animateFloatAsState(
        targetValue = data.angle,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150  //Redus pentru performanta
        )
    )
    val distAnim by animateFloatAsState(
        targetValue = data.distance,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150
        )
    )

    Column(
        modifier = modifier
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Informatii cu valori actualizate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Unghi: ${data.angle.toInt()}°",
                color = Color(0xFF00E5FF),
                fontSize = 14.sp,  //Font mai mic
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Distanta: ${data.distance.toInt()} cm",
                color = Color(0xFFFFD740),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        //Canvas pentru radar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = kotlin.math.min(cx, cy) - 15f

                //Fundal radar
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x11000000),
                            Color(0x33000000)
                        ),
                        center = Offset(cx, cy),
                        radius = maxR
                    ),
                    radius = maxR,
                    center = Offset(cx, cy)
                )

                //Cercuri concentrice
                for (i in 1..3) {
                    drawCircle(
                        color = Color(0x33FFFFFF),
                        radius = maxR * i / 3f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }

                //Linii radiale
                for (a in 0..180 step 30) {
                    val rad = a * 0.0174533f
                    val x = cx + maxR * cos(rad)
                    val y = cy - maxR * sin(rad)
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(cx, cy),
                        end = Offset(x, y),
                        strokeWidth = 1f
                    )
                }

                //Raza activa - foloseste valorile animate
                val angleRad = angleAnim * 0.0174533f
                val distNorm = (distAnim.coerceIn(0f, 250f) / 250f)
                val r = maxR * distNorm

                val px = cx + r * cos(angleRad)
                val py = cy - r * sin(angleRad)

                //Linia razei
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(cx, cy),
                    end = Offset(px, py),
                    strokeWidth = 2f
                )

                //Glow
                drawLine(
                    color = Color(0x3300E5FF),
                    start = Offset(cx, cy),
                    end = Offset(px, py),
                    strokeWidth = 6f
                )

                //Punctul de masurare
                drawCircle(
                    color = Color(0x3300E5FF),
                    radius = 10f,
                    center = Offset(px, py)
                )

                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 5f,
                    center = Offset(px, py)
                )
            }
        }
    }
}