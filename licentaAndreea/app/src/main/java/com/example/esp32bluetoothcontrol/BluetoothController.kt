package com.example.esp32bluetoothcontrol
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothController {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    //Callback pentru toate mesajele primite (POS / OBS / RADAR / PING)
    var onMessageReceived: ((String) -> Unit)? = null

    private val espName = "ESP32-4x4-Car"
    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    private var listeningJob: Job? = null

    //Stare de conexiune vizibila in UI
    val isConnected = mutableStateOf(false)

    //---------------------------------------------------------
    //Conectare la ESP32
    //---------------------------------------------------------
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val device: BluetoothDevice? = bluetoothAdapter
                ?.bondedDevices
                ?.firstOrNull { it.name == espName }

            if (device == null) {
                isConnected.value = false
                return@withContext false
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter?.cancelDiscovery()
            bluetoothSocket?.connect()

            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            isConnected.value = true
            startListening()

            return@withContext true

        } catch (e: IOException) {
            e.printStackTrace()
            isConnected.value = false
            return@withContext false
        }
    }

    //---------------------------------------------------------
    //Ascultare date de la ESP32 (POS / OBS / RADAR / PING)
    //---------------------------------------------------------
    private fun startListening() {
        listeningJob?.cancel()

        listeningJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = inputStream?.bufferedReader() ?: return@launch

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val msg = line.trim()

                    if (msg.isNotEmpty()) {
                        onMessageReceived?.invoke(msg)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                //daca iesim din while, conexiunea nu mai e valida
                isConnected.value = false
            }
        }
    }

    //---------------------------------------------------------
    //Trimitere comenzile (F/B/L/R/S/A/M)
    //---------------------------------------------------------
    fun sendCommand(cmd: String) {
        try {
            if (!isConnected.value) return
            outputStream?.write(cmd.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
            isConnected.value = false
        }
    }

    //---------------------------------------------------------
    //Deconectare
    //---------------------------------------------------------
    fun disconnect() {
        try {
            listeningJob?.cancel()
            bluetoothSocket?.close()
        } catch (_: IOException) {
        } finally {
            isConnected.value = false
        }
    }
}
