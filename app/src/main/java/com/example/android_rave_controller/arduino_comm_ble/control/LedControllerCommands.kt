// src/main/java/com/example/android_rave_controller/arduino_comm_ble/LedControllerCommands.kt
package com.example.android_rave_controller.arduino_comm_ble.control

object LedControllerCommands {
    const val CMD_GET_LED_COUNT = 0x0D
    const val CMD_GET_ALL_SEGMENT_CONFIGS = 0x0E
    const val CMD_SET_ALL_SEGMENT_CONFIGS = 0x0F // New
    const val CMD_GET_ALL_EFFECTS = 0x10         // New
    const val CMD_SAVE_CONFIG = 0x12             // New
    // Response codes
    const val CMD_ACK = 0xA0
}