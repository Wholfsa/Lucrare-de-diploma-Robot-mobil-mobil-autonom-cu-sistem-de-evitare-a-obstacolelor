package com.example.esp32bluetoothcontrol
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp  //Import corect pentru Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun DirectionPad(
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Buton SUS (Forward)
        DirectionButton(
            label = "↑",
            onClick = onForward
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Buton STANGA (Left)
            DirectionButton(
                label = "←",
                onClick = onLeft
            )

            //Buton STOP (centru) - putin mai mare
            DirectionButton(
                label = "■",
                onClick = onStop,
                size = 50.dp
            )

            //Buton DREAPTA (Right)
            DirectionButton(
                label = "→",
                onClick = onRight
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        //Buton JOS (Backward)
        DirectionButton(
            label = "↓",
            onClick = onBackward
        )
    }
}

@Composable
fun DirectionButton(
    label: String,
    onClick: () -> Unit,
    size: Dp = 45.dp  //Dimensiune mai mica, folosim Dp
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = tween(100)
    )

    Button(
        onClick = {
            pressed = true
            onClick()
            scope.launch {
                delay(150)
                pressed = false
            }
        },
        modifier = Modifier
            .size(size)
            .scale(scale)
            .graphicsLayer {
                shadowElevation = if (pressed) 2f else 6f
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7B1FA2),  //Mov inchis
            contentColor = Color.White
        ),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            label,
            fontSize = if (label == "■") 20.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}