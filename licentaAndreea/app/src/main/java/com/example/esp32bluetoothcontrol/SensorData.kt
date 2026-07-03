package com.example.esp32bluetoothcontrol

data class SensorData(
    val angle: Float = 0f,
    val distance: Float = 0f,
    val points: List<MapPoint> = emptyList()
)

