package com.example.android_rave_controller.arduino_comm_ble.control

import com.example.android_rave_controller.arduino_comm_ble.BluetoothConnectionManager
import java.util.LinkedList
import java.util.Queue

class CommandQueue(private val connectionManager: BluetoothConnectionManager) {
    private val commandQueue: Queue<ByteArray> = LinkedList()
    private var isSendingCommand = false

    @Synchronized
    fun queueCommand(command: ByteArray) {
        commandQueue.add(command)
        if (!isSendingCommand) {
            sendNextCommand()
        }
    }

    @Synchronized
    fun onCommandSent() {
        isSendingCommand = false
        sendNextCommand()
    }

    @Synchronized
    private fun sendNextCommand() {
        if (commandQueue.isNotEmpty() && !isSendingCommand) {
            isSendingCommand = true
            val command = commandQueue.poll()
            if (command != null) {
                connectionManager.sendCommand(command)
            }
        }
    }
}