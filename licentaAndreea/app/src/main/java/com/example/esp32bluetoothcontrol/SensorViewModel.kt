package com.example.esp32bluetoothcontrol
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlin.math.abs

data class RobotPose(
    val x: Float = 0f,
    val y: Float = 0f,
    val theta: Float = 0f
)

class SensorViewModel : ViewModel() {

    private val _sensorData = mutableStateOf(SensorData())
    val sensorData = _sensorData

    val autoEnabled = mutableStateOf(false)
    val obstaclePoints = mutableStateListOf<MapPoint>()

    private val _robotPose = mutableStateOf(RobotPose())
    val robotPose = _robotPose

    val lastHeartbeat = mutableStateOf(0L)

    private var cleanupJob: Job? = null
    private var currentAngle = 90  //Ultimul unghi scanat

    init {
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(5000)
                cleanupPoints()
            }
        }
    }

    fun processMessage(msg: String) {
        try {
            when {
                //---------------- RADAR ----------------
                msg.startsWith("RADAR:") -> {
                    val p = msg.removePrefix("RADAR:").split(",")
                    if (p.size == 2) {
                        val angle = p[0].toFloatOrNull() ?: 0f
                        val dist = p[1].toFloatOrNull() ?: 0f
                        currentAngle = angle.toInt()

                        if (angle in 0f..180f && dist in 0f..250f) {
                            _sensorData.value = SensorData(
                                angle = angle,
                                distance = dist,
                                points = _sensorData.value.points
                            )
                        }
                    }
                }

                //---------------- POSITION (FIX: citește X, Y și theta) ----------------
                msg.startsWith("POS:") -> {
                    val p = msg.removePrefix("POS:").split(",")
                    if (p.size == 3) {
                        val x = p[0].toFloatOrNull() ?: _robotPose.value.x
                        val y = p[1].toFloatOrNull() ?: _robotPose.value.y
                        val theta = p[2].toFloatOrNull() ?: _robotPose.value.theta
                        _robotPose.value = RobotPose(x = x, y = y, theta = theta)
                    }
                }

                //---------------- OBSTACLE (coordonate globale) ----------------
                msg.startsWith("OBS:") -> {
                    val p = msg.removePrefix("OBS:").split(",")
                    if (p.size == 2) {
                        val x = p[0].toFloatOrNull() ?: 0f
                        val y = p[1].toFloatOrNull() ?: 0f

                        //Ignora punctele cu valori 0 sau prea mari
                        if ((abs(x) > 0.1f || abs(y) > 0.1f) && abs(x) < 1000f && abs(y) < 1000f) {
                            val newPoint = MapPoint(x, y)

                            //Verifica daca exista deja un punct foarte aproape
                            val exists = obstaclePoints.any {
                                abs(it.x - x) < 2f && abs(it.y - y) < 2f
                            }

                            if (!exists) {
                                //Limiteaza la 200 de puncte maxime
                                if (obstaclePoints.size >= 200) {
                                    obstaclePoints.removeAt(0)
                                }
                                obstaclePoints.add(newPoint)
                            }
                        }
                    }
                }

                //---------------- MODE ----------------
                msg == "AUTO_MODE_ON" -> {
                    autoEnabled.value = true
                    clearMap()
                    _robotPose.value = RobotPose()
                }

                msg == "MANUAL_MODE" -> {
                    autoEnabled.value = false
                }

                //---------------- HEARTBEAT ----------------
                msg == "PING" -> {
                    lastHeartbeat.value = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            println("Error processing message: $e")
        }
    }

    private fun cleanupPoints() {
        //Pastreaza doar ultimele 150 de puncte
        while (obstaclePoints.size > 150) {
            obstaclePoints.removeAt(0)
        }
    }

    fun clearMap() {
        obstaclePoints.clear()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupJob?.cancel()
    }
}