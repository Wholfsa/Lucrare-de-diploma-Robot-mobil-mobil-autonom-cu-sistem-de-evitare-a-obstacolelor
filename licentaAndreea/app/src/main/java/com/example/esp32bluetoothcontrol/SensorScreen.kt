package com.example.esp32bluetoothcontrol
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SensorScreen(
    bluetoothController: BluetoothController,
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        bluetoothController.onMessageReceived = { msg ->
            sensorViewModel.processMessage(msg)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF12001F))
            .padding(16.dp)
    ) {

        Text("Radar", color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            RadarView(sensorViewModel = sensorViewModel)
        }

        Spacer(Modifier.height(16.dp))

        Text("Hartă reală", color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            RealMapView(sensorViewModel = sensorViewModel)
        }
    }
}
