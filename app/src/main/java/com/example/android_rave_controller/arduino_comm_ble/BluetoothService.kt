package com.example.android_rave_controller.arduino_comm_ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.LiveData
import com.example.android_rave_controller.arduino_comm_ble.control.CommandQueue
import com.example.android_rave_controller.arduino_comm_ble.control.DeviceProtocolHandler

@SuppressLint("MissingPermission")
object BluetoothService {

    private lateinit var connectionManager: BluetoothConnectionManager
    private lateinit var commandQueue: CommandQueue
    lateinit var deviceProtocolHandler: DeviceProtocolHandler

    // --- MODIFICATION START ---
    // Keep only the combined connectionStatus LiveData
    val connectionStatus: LiveData<ConnectionStatus>
        get() = connectionManager.connectionStatus
    // --- MODIFICATION END ---

    fun initialize(context: Context) {
        deviceProtocolHandler = DeviceProtocolHandler(context)
        connectionManager = BluetoothConnectionManager(context, deviceProtocolHandler)
        commandQueue = CommandQueue(connectionManager)
        deviceProtocolHandler.setCommandQueue(commandQueue)
    }

    fun connect(device: BluetoothDevice) {
        connectionManager.connect(device)
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun sendCommand(data: ByteArray) {
        commandQueue.queueCommand(data)
    }
}