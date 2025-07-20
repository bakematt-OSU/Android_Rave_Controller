package com.example.android_rave_controller.arduino_comm_ble

object CommandGetters {

    fun requestDeviceStatus() {
        CommandQueue.queueCommand(byteArrayOf(LedControllerCommands.CMD_GET_STATUS.toByte()))
    }

    fun requestLedCount() {
        CommandQueue.queueCommand(byteArrayOf(LedControllerCommands.CMD_GET_LED_COUNT.toByte()))
    }

    fun requestAllEffects() {
        CommandQueue.queueCommand(byteArrayOf(LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()))
    }
}