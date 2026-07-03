package com.example.esp32bluetoothcontrol
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun rememberBluetoothSensorData(
    bluetoothController: BluetoothController,
    sensorViewModel: SensorViewModel
) {
    LaunchedEffect(true) {

        bluetoothController.onMessageReceived = { msg ->
            //Trimitem mesajul direct în ViewModel
            sensorViewModel.processMessage(msg)
        }
    }
}
