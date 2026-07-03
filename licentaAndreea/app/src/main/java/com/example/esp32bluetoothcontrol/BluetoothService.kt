package com.example.robot
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class BluetoothService(
    private val socket: BluetoothSocket
) {
    private var onMessage: ((String) -> Unit)? = null

    fun onMessageReceived(callback: (String) -> Unit) {
        onMessage = callback
        startListening()
    }

    private fun startListening() {
        CoroutineScope(Dispatchers.IO).launch {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            while (true) {
                val line = reader.readLine()
                if (line != null) {
                    onMessage?.invoke(line.trim())
                }
            }
        }
    }

    fun send(cmd: String) {
        socket.outputStream.write(cmd.toByteArray())
    }
}
