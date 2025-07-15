package com.example.android_rave_controller.arduino_comm.bluetooth

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.android_rave_controller.arduino_comm.LedControllerCommands
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.RaveConfigurationForTransport
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentForTransport
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.LinkedList
import java.util.Queue
import kotlin.text.iterator

object DeviceProtocolHandler {

    private const val CMD_GET_STATUS: Byte = LedControllerCommands.CMD_GET_STATUS.toByte()
    private const val CMD_BATCH_CONFIG: Byte = LedControllerCommands.CMD_BATCH_CONFIG.toByte()
    private const val CMD_ACK: Byte = 0xA0.toByte()
    private const val CMD_CLEAR_SEGMENTS: Byte = LedControllerCommands.CMD_CLEAR_SEGMENTS.toByte()

    private val responseBuffer = StringBuilder()
    private val gson = Gson()
    private val commandQueue: Queue<ByteArray> = LinkedList()
    private var isSendingCommand = false
    private var openBraceCount = 0

    fun requestDeviceStatus() {
        responseBuffer.clear()
        openBraceCount = 0
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

    fun parseResponse(bytes: ByteArray) {
        if (bytes.size == 1) {
            when (bytes[0]) {
                CMD_ACK -> {
                    Log.d("ProtocolHandler", "ACK received. Sending next command.")
                    onCommandSent()
                    return
                }
                0.toByte() -> {
                    Log.d("ProtocolHandler", "Heartbeat received and discarded.")
                    return
                }
            }
        }

        val incomingText = bytes.toString(Charsets.UTF_8)
        for (char in incomingText) {
            if (char == '{') {
                if (openBraceCount == 0) {
                    responseBuffer.clear()
                }
                openBraceCount++
            }
            if (openBraceCount > 0) {
                responseBuffer.append(char)
            }
            if (char == '}') {
                openBraceCount--
                if (openBraceCount == 0 && responseBuffer.isNotEmpty()) {
                    val jsonString = responseBuffer.toString()
                    Log.d("ProtocolHandler", "Complete JSON received: $jsonString")
                    try {
                        val status = gson.fromJson(jsonString, Status::class.java)

                        // **FIX**: Safely handle the nullable effects list
                        val effectObjects = status.effects?.map { effectName ->
                            Effect(name = effectName)
                        } ?: emptyList() // Use an empty list if effects is null

                        Handler(Looper.getMainLooper()).post {
                            EffectsRepository.updateEffects(effectObjects)
                            SegmentsRepository.updateSegments(status.segments)
                        }
                        Log.d("ProtocolHandler", "Successfully parsed and updated status.")
                    } catch (e: JsonSyntaxException) {
                        Log.e("ProtocolHandler", "Failed to parse status JSON: ${e.message}")
                    } finally {
                        responseBuffer.clear()
                    }
                }
            }
        }
        if (openBraceCount > 0) {
            Log.d("ProtocolHandler", "Partial message received. Buffer content: $responseBuffer")
        }
    }
}