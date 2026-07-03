package com.example.esp32bluetoothcontrol
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),   //Albastru modern
                        Color(0xFF6A1B9A)    //Mov elegant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {

            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(120.dp)
            )


            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ROBOT MOBIL AUTONOM",
                style = MaterialTheme.typography.h4,
                color = Color.White,
                textAlign= TextAlign.Center

            )

            Text(
                text = "cu evitare de obstacole",
                style = MaterialTheme.typography.subtitle1,
                color = Color.White.copy(alpha = 0.85f),
                textAlign=TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            val scale = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                while (true) {
                    scale.animateTo(
                        targetValue = 1.08f,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                    )
                }
            }

            Button(
                onClick = { navController.navigate("control") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White
                )
            ) {
                Text(
                    text = "START",
                    color = Color(0xFF1E88E5),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}
