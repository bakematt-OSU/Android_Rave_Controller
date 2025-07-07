package com.example.android_rave_controller

import android.util.Log
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object DeviceProtocolHandler {

    // --- Command IDs ---
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

    // --- Restored functions for UI compatibility ---
    fun setSegmentRange(segIdx: Int, start: Int, end: Int) {
        val startHigh = (start shr 8).toByte()
        val startLow = (start and 0xFF).toByte()
        val endHigh = (end shr 8).toByte()
        val endLow = (end and 0xFF).toByte()
        val command = byteArrayOf(0x07, segIdx.toByte(), startHigh, startLow, endHigh, endLow)
        BluetoothService.sendCommand(command)
    }

    fun selectSegment(segIdx: Int) {
        val command = byteArrayOf(0x05, segIdx.toByte())
        BluetoothService.sendCommand(command)
    }

    fun setEffect(effectIndex: Int) {
        val command = byteArrayOf(0x02, effectIndex.toByte())
        BluetoothService.sendCommand(command)
    }

    fun setSegmentBrightness(segIdx: Int, brightness: Int) {
        val command = byteArrayOf(0x04, segIdx.toByte(), brightness.toByte())
        BluetoothService.sendCommand(command)
    }


    // --- Public Function to Parse Incoming Data ---
    fun parseResponse(bytes: ByteArray) {
        // **THIS IS THE FINAL FIX**
        // Check if the message is a single-byte heartbeat (value 0). If so, ignore it.
        if (bytes.size == 1 && bytes[0] == 0.toByte()) {
            Log.d("ProtocolHandler", "Heartbeat received and discarded.")
            return // Exit the function immediately.
        }

        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)

        if (incomingText.contains('\n')) {
            var fullMessage = responseBuffer.toString().substringBefore('\n')
            Log.d("ProtocolHandler", "Received raw message: $fullMessage")

            // Find the first '{' and trim any garbage characters from the start.
            val jsonStartIndex = fullMessage.indexOf('{')
            if (jsonStartIndex != -1) {
                fullMessage = fullMessage.substring(jsonStartIndex)
            }

            Log.d("ProtocolHandler", "Cleaned message for parsing: $fullMessage")

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