// src/main/java/com/example/android_rave_controller/arduino_comm_ble/LedControllerCommands.kt
package com.example.android_rave_controller.arduino_comm_ble.control

object LedControllerCommands {
    // Explicitly define command constants as Byte type
    const val CMD_GET_LED_COUNT: Byte = 0x0D.toByte()
    const val CMD_GET_ALL_SEGMENT_CONFIGS: Byte = 0x0E.toByte()
    const val CMD_SET_ALL_SEGMENT_CONFIGS: Byte = 0x0F.toByte() // New
    const val CMD_GET_ALL_EFFECTS: Byte = 0x10.toByte()         // New
    const val CMD_SAVE_CONFIG: Byte = 0x12.toByte()             // New
    // Response codes
    const val CMD_ACK_GENERIC: Byte = 0xA0.toByte() // Explicitly define as Byte
}
