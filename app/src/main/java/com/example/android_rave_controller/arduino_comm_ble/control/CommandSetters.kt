package com.example.android_rave_controller.arduino_comm_ble.control

import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentsRepository
import com.google.gson.Gson
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CommandSetters {

    // --- MODIFIED ---
    fun sendFullConfiguration(segments: List<Segment>) {
        BluetoothService.deviceProtocolHandler.startSendingFullConfiguration(segments)
    }

    fun saveConfigurationToDevice() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SAVE_CONFIG.toByte()))
    }

}