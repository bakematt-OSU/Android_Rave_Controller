package com.example.android_rave_controller

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.RaveConfiguration
import com.example.android_rave_controller.models.RaveConfigurationForTransport
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentForTransport
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.LinkedList
import java.util.Queue

object DeviceProtocolHandler {

    // --- Command IDs ---
    private const val CMD_GET_STATUS: Byte = 0x08
    private const val CMD_BATCH_CONFIG: Byte = 0x09
    private const val CMD_ACK: Byte = 0xA0.toByte()
    private const val CMD_CLEAR_SEGMENTS: Byte = 0x06


    // Buffer for assembling fragmented Bluetooth packets
    private val responseBuffer = StringBuilder()
    private val gson = Gson()
    private val commandQueue: Queue<ByteArray> = LinkedList()
    private var isSendingCommand = false


    // --- Public Functions to Send Commands ---
    fun requestDeviceStatus() {
        responseBuffer.clear()
        val command = byteArrayOf(CMD_GET_STATUS)
        commandQueue.add(command)
        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
    }

    fun onCommandSent() {
        isSendingCommand = false
        sendNextCommandFromQueue()
    }

    private fun sendNextCommandFromQueue() {
        if (commandQueue.isEmpty()) {
            isSendingCommand = false
            return
        }

        isSendingCommand = true
        val command = commandQueue.poll()
        if (command != null) {
            BluetoothService.sendCommand(command)
        }
    }

    fun sendFullConfiguration(segments: List<Segment>, effects: List<String>) {
        commandQueue.clear()

        // --- NEW: Convert segments to a transport-friendly format with an effect index ---
        val transportSegments = segments.map { segment ->
            SegmentForTransport(
                id = segment.id,
                name = segment.name,
                startLed = segment.startLed,
                endLed = segment.endLed,
                brightness = segment.brightness,
                effectIndex = effects.indexOf(segment.effect).takeIf { it != -1 } ?: 0
            )
        }
        val raveConfigForTransport = RaveConfigurationForTransport(transportSegments, effects)
        val jsonPayload = gson.toJson(raveConfigForTransport)
        // --- END NEW ---

        val fullMessage = byteArrayOf(CMD_BATCH_CONFIG) + jsonPayload.toByteArray(Charsets.UTF_8)

        val chunkSize = 20
        for (i in fullMessage.indices step chunkSize) {
            val end = (i + chunkSize).coerceAtMost(fullMessage.size)
            val chunk = fullMessage.sliceArray(i until end)
            commandQueue.add(chunk)
        }

        Log.d("ProtocolHandler", "Queued ${commandQueue.size} chunks for batch sending.")

        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
    }

    // --- Restored functions for individual debug commands ---
    fun setSegmentRange(segIdx: Int, start: Int, end: Int) {
        val startHigh = (start shr 8).toByte()
        val startLow = (start and 0xFF).toByte()
        val endHigh = (end shr 8).toByte()
        val endLow = (end and 0xFF).toByte()
        val command = byteArrayOf(0x07, segIdx.toByte(), startHigh, startLow, endHigh, endLow)
        commandQueue.add(command)
        if (!isSendingCommand) sendNextCommandFromQueue()
    }

    fun selectSegment(segIdx: Int) {
        val command = byteArrayOf(0x05, segIdx.toByte())
        commandQueue.add(command)
        if (!isSendingCommand) sendNextCommandFromQueue()
    }

    fun setEffect(effectIndex: Int) {
        val command = byteArrayOf(0x02, effectIndex.toByte())
        commandQueue.add(command)
        if (!isSendingCommand) sendNextCommandFromQueue()
    }

    fun setSegmentBrightness(segIdx: Int, brightness: Int) {
        val command = byteArrayOf(0x04, segIdx.toByte(), brightness.toByte())
        commandQueue.add(command)
        if (!isSendingCommand) sendNextCommandFromQueue()
    }

    // --- Public Function to Parse Incoming Data ---
    fun parseResponse(bytes: ByteArray) {
        if (bytes.size == 1) {
            when (bytes[0]) {
                CMD_ACK -> {
                    Log.d("ProtocolHandler", "ACK received. Sending next command.")
                    onCommandSent()
                    return // Exit after handling ACK
                }
                0.toByte() -> {
                    Log.d("ProtocolHandler", "Heartbeat received and discarded.")
                    return // Exit after handling Heartbeat
                }
            }
        }

        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)

        if (incomingText.contains('\n')) {
            var fullMessage = responseBuffer.toString().substringBefore('\n')
            Log.d("ProtocolHandler", "Received raw message: $fullMessage")

            val jsonStartIndex = fullMessage.lastIndexOf('{')
            if (jsonStartIndex != -1) {
                fullMessage = fullMessage.substring(jsonStartIndex)
            } else {
                Log.w("ProtocolHandler", "Received message does not contain JSON, discarding: $fullMessage")
                responseBuffer.clear()
                return
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