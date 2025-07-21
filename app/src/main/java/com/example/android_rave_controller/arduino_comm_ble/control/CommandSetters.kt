package com.example.android_rave_controller.arduino_comm_ble.control

import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentsRepository
import com.google.gson.Gson
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CommandSetters {

    private val gson = Gson()

    fun sendSingleSegment(segment: Segment) {
        val jsonPayload = gson.toJson(segment)
        val command = byteArrayOf(LedControllerCommands.CMD_SET_SINGLE_SEGMENT_JSON.toByte()) + jsonPayload.toByteArray(Charsets.UTF_8)
        BluetoothService.sendCommand(command)
    }

    fun sendFullConfiguration(segments: List<Segment>) {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SET_ALL_SEGMENT_CONFIGS.toByte()))
        val segmentCount = segments.size
        val countBytes = byteArrayOf((segmentCount shr 8).toByte(), (segmentCount and 0xFF).toByte())
        BluetoothService.sendCommand(countBytes)
        segments.forEach { segment ->
            BluetoothService.sendCommand(gson.toJson(segment).toByteArray(Charsets.UTF_8))
        }
    }

    fun saveConfigurationToDevice() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SAVE_CONFIG.toByte()))
    }

    fun clearSegmentsOnDevice() {
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_CLEAR_SEGMENTS.toByte()))
    }

    fun sendParameterUpdate(segmentId: Int, paramName: String, paramType: String, value: Any) {
        val segIndex = SegmentsRepository.segments.value?.indexOfFirst { it.id == segmentId } ?: -1
        if (segIndex == -1) return

        val paramTypeOrdinal = when (paramType) {
            "integer" -> 0
            "float" -> 1
            "color" -> 2
            "boolean" -> 3
            else -> return
        }
        val paramNameBytes = paramName.toByteArray(Charsets.UTF_8)
        val valueBytes = when (value) {
            is Int -> ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array()
            is Float -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
            is Boolean -> byteArrayOf(if (value) 1 else 0)
            else -> return
        }
        val commandPayload = byteArrayOf(segIndex.toByte(), paramTypeOrdinal.toByte(), paramNameBytes.size.toByte()) + paramNameBytes + valueBytes
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SET_EFFECT_PARAMETER.toByte()) + commandPayload)
    }

    fun setLedCount(count: Int) {
        val countBytes = byteArrayOf((count shr 8).toByte(), (count and 0xFF).toByte())
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SET_LED_COUNT.toByte()) + countBytes)
    }
}