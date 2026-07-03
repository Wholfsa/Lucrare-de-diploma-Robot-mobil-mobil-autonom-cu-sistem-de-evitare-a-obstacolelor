package com.example.esp32bluetoothcontrol
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ControlScreen(
    onConnect: () -> Unit,
    bluetoothController: BluetoothController,
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    //Snackbar pentru feedback vizual
    val snackbarHostState = remember { SnackbarHostState() }
    var connectionStatus by remember { mutableStateOf("Disconnected") }

    //Legam Bluetooth → ViewModel
    LaunchedEffect(Unit) {
        bluetoothController.onMessageReceived = { msg ->
            sensorViewModel.processMessage(msg)
        }
    }

    //Verificam heartbeat-ul si conexiunea reala
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val now = System.currentTimeMillis()
            val lastPing = sensorViewModel.lastHeartbeat.value

            val newStatus = when {
                !bluetoothController.isConnected.value -> "Disconnected"
                (lastPing != 0L && now - lastPing > 3000) -> "Disconnected"
                else -> "Connected"
            }

            if (newStatus != connectionStatus) {
                connectionStatus = newStatus

                //Daca statusul devine Disconnected → afisam snackbar
                if (newStatus == "Disconnected") {
                    snackbarHostState.showSnackbar("Robotul s-a deconectat")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
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
            //STATUS + CONNECT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $connectionStatus",
                    color = when (connectionStatus) {
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
                    )
                ) {
                    Text("Connect", color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            //RADAR
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
                    .height(260.dp)
            ) {
                RadarView(sensorViewModel = sensorViewModel)
            }

            Spacer(Modifier.height(8.dp))

            //HARTA REALA
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
                RealMapView(sensorViewModel = sensorViewModel)
            }

            Spacer(Modifier.height(8.dp))

            //CONTROL
            Text(
                text = "CONTROL",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            DirectionPad(
                onForward = { bluetoothController.sendCommand("F") },
                onBackward = { bluetoothController.sendCommand("B") },
                onLeft = { bluetoothController.sendCommand("L") },
                onRight = { bluetoothController.sendCommand("R") },
                onStop = { bluetoothController.sendCommand("S") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            //INDICATOR DIRECTIE
            DirectionIndicator(sensorViewModel = sensorViewModel)
        }
    }
}

@Composable
fun DirectionIndicator(
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    val pose by sensorViewModel.robotPose

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "↑",
            color = Color.Cyan,
            fontSize = 24.sp,
            modifier = Modifier.graphicsLayer {
                rotationZ = pose.theta
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Theta: ${pose.theta.toInt()}°",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
