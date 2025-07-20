package com.example.android_rave_controller.arduino_comm_ble.control

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.EffectsRepository
import kotlin.text.iterator

class DeviceProtocolHandler(private val context: Context) {

    val liveLedCount = MutableLiveData<Int>()
    private val responseBuffer = StringBuilder()
    private lateinit var commandQueue: CommandQueue

    fun setCommandQueue(queue: CommandQueue) {
        this.commandQueue = queue
    }

    fun onCommandSent() {
        commandQueue.onCommandSent()
    }

    fun parseResponse(bytes: ByteArray) {
        // Handle the initial effect count message
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()) {
            if (bytes.size >= 3) {
                val effectCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                // Acknowledge to start receiving the effect info
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
            }
            return
        }

        // Append incoming text to the buffer
        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)

        // Delegate buffer processing to the parser
        JsonResponseParser.processBuffer(responseBuffer,
            onEffectsReceived = { effects ->
                val currentEffects = EffectsRepository.effects.value?.toMutableList() ?: mutableListOf()
                effects.forEach { effect ->
                    val existingIndex = currentEffects.indexOfFirst { it.name == effect.name }
                    if (existingIndex != -1) {
                        currentEffects[existingIndex] = effect
                    } else {
                        currentEffects.add(effect)
                    }
                }
                EffectsRepository.updateEffects(currentEffects)
                // Acknowledge to get the next effect
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
            },
            onStatusReceived = { status ->
                // This part of your logic might need adjustment based on your app's flow.
                // For now, it continues to request all effects after receiving a status.
                CommandGetters.requestAllEffects()
            }
        )
    }
}