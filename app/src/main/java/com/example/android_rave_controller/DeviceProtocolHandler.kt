package com.example.android_rave_controller

import android.util.Log
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status // <-- IMPORTANT: Added import
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object DeviceProtocolHandler {

    // --- Command IDs ---
    private const val CMD_SET_EFFECT: Byte = 0x02
    private const val CMD_SET_SEG_BRIGHT: Byte = 0x04
    private const val CMD_SELECT_SEGMENT: Byte = 0x05
    private const val CMD_SET_SEG_RANGE: Byte = 0x07
    private const val CMD_GET_STATUS: Byte = 0x08

    // Buffer for assembling fragmented Bluetooth packets
    private val responseBuffer = StringBuilder()
    private val gson = Gson()

    // --- Public Functions to Send Commands ---
    fun requestDeviceStatus() {
        responseBuffer.clear()
        val command = byteArrayOf(CMD_GET_STATUS)
        BluetoothService.sendCommand(command)
    }

    // **RESTORED FUNCTIONS** that are still used by the UI
    fun setSegmentRange(segIdx: Int, start: Int, end: Int) {
        val startHigh = (start shr 8).toByte()
        val startLow = (start and 0xFF).toByte()
        val endHigh = (end shr 8).toByte()
        val endLow = (end and 0xFF).toByte()
        val command = byteArrayOf(CMD_SET_SEG_RANGE, segIdx.toByte(), startHigh, startLow, endHigh, endLow)
        BluetoothService.sendCommand(command)
    }

    fun selectSegment(segIdx: Int) {
        val command = byteArrayOf(CMD_SELECT_SEGMENT, segIdx.toByte())
        BluetoothService.sendCommand(command)
    }

    fun setEffect(effectIndex: Int) {
        val command = byteArrayOf(CMD_SET_EFFECT, effectIndex.toByte())
        BluetoothService.sendCommand(command)
    }

    fun setSegmentBrightness(segIdx: Int, brightness: Int) {
        val command = byteArrayOf(CMD_SET_SEG_BRIGHT, segIdx.toByte(), brightness.toByte())
        BluetoothService.sendCommand(command)
    }


    // --- Public Function to Parse Incoming Data ---
    fun parseResponse(bytes: ByteArray) {
        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)

        if (incomingText.contains('\n')) {
            val fullMessage = responseBuffer.toString().substringBefore('\n').trim()
            Log.d("ProtocolHandler", "Complete message received: $fullMessage")

            try {
                val status = gson.fromJson(fullMessage, Status::class.java)
                EffectsRepository.updateEffects(status.effects)
                SegmentsRepository.updateSegments(status.segments)
                Log.d("ProtocolHandler", "Successfully parsed status JSON.")
            } catch (e: JsonSyntaxException) {
                Log.e("ProtocolHandler", "Failed to parse status JSON", e)
            } finally {
                responseBuffer.clear()
            }
        } else {
            Log.d("ProtocolHandler", "Partial message received. Buffer content: $responseBuffer")
        }
    }
}