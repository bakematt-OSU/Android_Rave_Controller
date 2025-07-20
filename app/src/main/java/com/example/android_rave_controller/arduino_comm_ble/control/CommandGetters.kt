package com.example.android_rave_controller.arduino_comm_ble.control

import com.example.android_rave_controller.arduino_comm_ble.BluetoothService

object CommandGetters {

    fun requestDeviceStatus() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_GET_ALL_SEGMENT_CONFIGS.toByte()))
    }

    fun requestLedCount() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_GET_LED_COUNT.toByte()))
    }

    fun requestAllEffects() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()))
    }
}