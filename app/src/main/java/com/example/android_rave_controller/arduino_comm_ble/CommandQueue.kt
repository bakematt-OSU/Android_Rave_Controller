package com.example.android_rave_controller.arduino_comm_ble

import java.util.LinkedList
import java.util.Queue

object CommandQueue {
    private val commandQueue: Queue<ByteArray> = LinkedList()
    private var isSendingCommand = false

    fun queueCommand(command: ByteArray) {
        commandQueue.add(command)
        if (!isSendingCommand) {
            sendNextCommand()
        }
    }

    fun onCommandSent() {
        isSendingCommand = false
        sendNextCommand()
    }

    private fun sendNextCommand() {
        if (commandQueue.isNotEmpty() && !isSendingCommand) {
            isSendingCommand = true
            val command = commandQueue.poll()
            if (command != null) {
                BluetoothService.sendCommand(command)
            }
        }
    }
}