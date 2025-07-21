// src/main/java/com/example/android_rave_controller/arduino_comm_ble/LedControllerCommands.kt
package com.example.android_rave_controller.arduino_comm_ble.control

object LedControllerCommands {
    const val CMD_CLEAR_SEGMENTS = 0x06
    const val CMD_SET_EFFECT_PARAMETER = 0x0A
    const val CMD_GET_EFFECT_INFO = 0x0B
    const val CMD_SET_LED_COUNT = 0x0C
    const val CMD_GET_LED_COUNT = 0x0D
    const val CMD_GET_ALL_SEGMENT_CONFIGS = 0x0E
    const val CMD_SET_ALL_SEGMENT_CONFIGS = 0x0F // New
    const val CMD_GET_ALL_EFFECTS = 0x10         // New
    const val CMD_SET_SINGLE_SEGMENT_JSON = 0x11 // New
    const val CMD_SAVE_CONFIG = 0x12             // New
    const val CMD_READY = 0xD0.toByte()          // New: Device ready signal


    // Response codes
    const val CMD_ACK = 0xA0
}