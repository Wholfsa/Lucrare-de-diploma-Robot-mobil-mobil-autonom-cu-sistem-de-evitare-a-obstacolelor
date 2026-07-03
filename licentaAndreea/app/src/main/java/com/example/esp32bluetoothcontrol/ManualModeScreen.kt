package com.example.esp32bluetoothcontrol
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ManualModeScreen(
    connectionStatus: String,
    onConnect: () -> Unit,
    bluetoothController: BluetoothController,
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    //Legam Bluetooth → ViewModel
    LaunchedEffect(Unit) {
        bluetoothController.onMessageReceived = { msg ->
            sensorViewModel.processMessage(msg)
        }
    }

    //Verificam heartbeat-ul si conexiunea reala
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
                        Color(0xFF1E88E5),   //Albastru modern
                        Color(0xFF6A1B9A)    //Mov elegant
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
                shape = MaterialTheme.shapes.small
            ) {
                Text("Connect", color = Color.White)
            }
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

        Spacer(Modifier.height(8.dp))

        //============ CONTROL ============
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
                .padding(bottom = 8.dp)
        )
    }
}
