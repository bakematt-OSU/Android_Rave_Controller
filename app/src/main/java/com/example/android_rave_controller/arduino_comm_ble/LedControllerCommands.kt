// src/main/java/com/example/android_rave_controller/arduino_comm_ble/LedControllerCommands.kt
package com.example.android_rave_controller.arduino_comm_ble

object LedControllerCommands {
    const val CMD_SET_COLOR = 0x01
    const val CMD_SET_EFFECT = 0x02
    const  val CMD_SET_BRIGHTNESS = 0x03
    const val CMD_SET_SEG_BRIGHT = 0x04
    const val CMD_SELECT_SEGMENT = 0x05
    const val CMD_CLEAR_SEGMENTS = 0x06
    const val CMD_SET_SEG_RANGE = 0x07
    const val CMD_GET_STATUS = 0x08
    const val CMD_BATCH_CONFIG = 0x09
    const val CMD_SET_EFFECT_PARAMETER = 0x0A // Added: New command for setting effect parameters
    const val CMD_NUM_PIXELS = 0x0A // Note: Original CMD_NUM_PIXELS at 0x0A might conflict if used simultaneously. CMD_SET_EFFECT_PARAMETER is now 0x0A in BinaryCommandHandler.h.
    const val CMD_GET_EFFECT_INFO = 0x0B
    const val CMD_SET_LED_COUNT = 0x0C
    const val CMD_GET_LED_COUNT = 0x0D
    // Added CMD_ACK based on usage in DeviceProtocolHandler.kt for responses
    // 0xA0 is a common value for acknowledgment in embedded systems and often
    // used as a response prefix in some protocols.
    const val CMD_ACK = 0xA0
}