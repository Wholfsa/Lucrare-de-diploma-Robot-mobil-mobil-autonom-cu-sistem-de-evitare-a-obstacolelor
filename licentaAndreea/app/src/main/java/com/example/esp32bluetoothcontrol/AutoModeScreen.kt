package com.example.esp32bluetoothcontrol
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun AutoModeScreen(
    connectionStatus: String,
    onConnect: () -> Unit,
    bluetoothController: BluetoothController,
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        bluetoothController.onMessageReceived = { msg ->
            sensorViewModel.processMessage(msg)
        }
    }

    var isAutoRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf(connectionStatus) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val now = System.currentTimeMillis()
            val lastPing = sensorViewModel.lastHeartbeat.value

            status = when {
                !bluetoothController.isConnected.value -> "Disconnected"
                (lastPing != 0L && now - lastPing > 3000) -> "Disconnected"
                else -> "Connected"
            }
        }
    }

    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E88E5),
                        Color(0xFF6A1B9A)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        //============ STATUS + CONNECT ============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Status: $status",
                color = when (status) {
                    "Connected" -> Color.Green
                    "Disconnected" -> Color.Red
                    else -> Color.Yellow
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    onConnect()
                    sensorViewModel.lastHeartbeat.value = System.currentTimeMillis()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Connect", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        //============ CONTROL AUTOMAT ============
        Text(
            text = "CONTROL AUTOMAT",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        //============ RANDUL 1: START / STOP ============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AutoButton(
                label = "START",
                onClick = {
                    isAutoRunning = true
                    bluetoothController.sendCommand("A")
                },
                isRunning = isAutoRunning,
                modifier = Modifier.weight(1f)
            )

            AutoButton(
                label = "STOP",
                onClick = {
                    isAutoRunning = false
                    bluetoothController.sendCommand("M")
                },
                isRunning = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        //============ RANDUL 2: RESET ============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AutoButton(
                label = "RESET",
                onClick = {
                    sensorViewModel.clearMap()
                },
                isRunning = false,
                modifier = Modifier.width(160.dp),
                buttonColor = Color(0xFFFF5722)
            )
        }

        Spacer(Modifier.height(12.dp))

        //============ RADAR ============
        Text(
            text = "RADAR",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            RadarView(
                sensorViewModel = sensorViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        //============ HARTA REALA ============
        Text(
            text = "HARTA REALA",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            RealMapView(
                sensorViewModel = sensorViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isAutoRunning) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🔄 MOD PATROLARE ACTIV",
                color = Color(0xFF00E676),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x2200E676),
                                Color(0x4400E676),
                                Color(0x2200E676)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

//============================================
//AUTO BUTTON COMPONENT
//============================================

@Composable
fun AutoButton(
    label: String,
    onClick: () -> Unit,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    buttonColor: Color = Color(0xFF7B1FA2)
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = tween(100)
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isRunning && label == "START") 1.05f else 1f,
        animationSpec = tween(500, easing = androidx.compose.animation.core.EaseInOut)
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
        modifier = modifier
            .scale(if (isRunning && label == "START") pulseScale else scale)
            .graphicsLayer {
                shadowElevation = if (pressed) 4f else 8f
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning && label == "START")
                Color(0xFFAB47BC) else buttonColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            label,
            fontSize = if (label == "RESET") 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
