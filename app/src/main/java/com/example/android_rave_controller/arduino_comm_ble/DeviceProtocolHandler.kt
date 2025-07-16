// src/main/java/com/example/android_rave_controller/arduino_comm_ble/DeviceProtocolHandler.kt
package com.example.android_rave_controller.arduino_comm_ble

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.RaveConfigurationForTransport
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentForTransport
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import kotlin.text.iterator

object DeviceProtocolHandler {

    private val CMD_GET_STATUS: Byte = LedControllerCommands.CMD_GET_STATUS.toByte()
    private val CMD_BATCH_CONFIG: Byte = LedControllerCommands.CMD_BATCH_CONFIG.toByte()
    private val CMD_ACK: Byte = LedControllerCommands.CMD_ACK.toByte()
    private val CMD_CLEAR_SEGMENTS: Byte = LedControllerCommands.CMD_CLEAR_SEGMENTS.toByte()
    private val CMD_GET_LED_COUNT: Byte = LedControllerCommands.CMD_GET_LED_COUNT.toByte()
    private val CMD_SET_LED_COUNT: Byte = LedControllerCommands.CMD_SET_LED_COUNT.toByte()
    private val CMD_GET_EFFECT_INFO: Byte = LedControllerCommands.CMD_GET_EFFECT_INFO.toByte()
    private val CMD_SET_EFFECT_PARAMETER: Byte = LedControllerCommands.CMD_SET_EFFECT_PARAMETER.toByte() // New: Reference the new command

    private val responseBuffer = StringBuilder()
    private val gson = Gson()
    private val commandQueue: Queue<ByteArray> = LinkedList()
    private var isSendingCommand = false
    private var openBraceCount = 0

    val liveLedCount = MutableLiveData<Int>()

    private lateinit var applicationContext: Context
    fun initialize(context: Context) {
        applicationContext = context
    }

    fun requestDeviceStatus() {
        responseBuffer.clear()
        openBraceCount = 0
        val command = byteArrayOf(CMD_GET_STATUS)
        // Add to front of queue if it's a high-priority command or if we want to ensure it's sent next
        // For initial status, it's good to prioritize.
        commandQueue.add(command)
        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
    }

    // This function is called by BluetoothService when a write operation completes successfully.
    fun onCommandSent() {
        isSendingCommand = false // Mark the current command as finished
        sendNextCommandFromQueue() // Try to send the next command in the queue
    }

    private fun sendNextCommandFromQueue() {
        if (commandQueue.isEmpty()) {
            isSendingCommand = false
            return
        }
        if (isSendingCommand) {
            // A command is already in flight, wait for its completion callback.
            return
        }
        isSendingCommand = true
        val command = commandQueue.poll()
        if (command != null) {
            BluetoothService.sendCommand(command)
        }
    }

    fun sendFullConfiguration(segments: List<Segment>, effects: List<String>) {
        commandQueue.clear() // Clear existing commands if sending a full config
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

    fun requestEffectParameters(effectName: String) {
        val effectId = EffectsRepository.effects.value?.indexOfFirst { it.name == effectName } ?: -1

        if (effectId != -1) {
            val command = byteArrayOf(CMD_GET_EFFECT_INFO, 0x00, effectId.toByte()) // Assuming segment 0, effect ID
            commandQueue.add(command)
            if (!isSendingCommand) {
                sendNextCommandFromQueue()
            }
        } else {
            Log.e("ProtocolHandler", "Effect '$effectName' not found or has no ID.")
        }
    }

    // New function to send parameter updates
    fun sendParameterUpdate(segmentId: String, paramName: String, paramType: String, value: Any) {
        val segIndex = SegmentsRepository.segments.value?.indexOfFirst { it.id == segmentId } ?: -1
        if (segIndex == -1) {
            Log.e("ProtocolHandler", "Segment with ID '$segmentId' not found.")
            return
        }

        val paramTypeOrdinal = when (paramType) {
            "integer" -> 0 // ParamType::INTEGER ordinal
            "float" -> 1    // ParamType::FLOAT ordinal
            "color" -> 2    // ParamType::COLOR ordinal
            "boolean" -> 3  // ParamType::BOOLEAN ordinal
            else -> {
                Log.e("ProtocolHandler", "Unknown parameter type: $paramType")
                return
            }
        }

        val paramNameBytes = paramName.toByteArray(Charsets.UTF_8)
        val nameLen = paramNameBytes.size.toByte()

        val valueBytes = when (paramType) {
            "integer" -> {
                val intValue = value as Int
                ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(intValue).array()
            }
            "float" -> {
                val floatValue = value as Float
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(floatValue).array() // Assuming Arduino uses little-endian for float
            }
            "color" -> {
                val colorValue = value as Int // Android color is ARGB
                byteArrayOf(
                    0x00, // Alpha byte (unused by Arduino's parsing in BinaryCommandHandler.cpp)
                    (colorValue shr 16 and 0xFF).toByte(), // Red
                    (colorValue shr 8 and 0xFF).toByte(),  // Green
                    (colorValue and 0xFF).toByte()         // Blue
                )
            }
            "boolean" -> {
                val boolValue = value as Boolean
                byteArrayOf(if (boolValue) 0x01 else 0x00)
            }
            else -> byteArrayOf()
        }

        val commandPayload = byteArrayOf(
            segIndex.toByte(),
            paramTypeOrdinal.toByte(),
            nameLen
        ) + paramNameBytes + valueBytes

        val fullCommand = byteArrayOf(CMD_SET_EFFECT_PARAMETER) + commandPayload

        commandQueue.add(fullCommand)
        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
        Log.d("ProtocolHandler", "Queued parameter update for Segment: $segIndex, Param: $paramName ($paramType) = $value")
    }

    fun requestLedCount() {
        val command = byteArrayOf(CMD_GET_LED_COUNT)
        commandQueue.add(command)
        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
    }

    fun setLedCount(count: Int) {
        val countBytes = byteArrayOf(
            CMD_SET_LED_COUNT,
            (count shr 8).toByte(),
            (count and 0xFF).toByte()
        )
        commandQueue.add(countBytes)
        if (!isSendingCommand) {
            sendNextCommandFromQueue()
        }
    }

    fun parseResponse(bytes: ByteArray) {
        if (bytes.isNotEmpty()) {
            when (bytes[0]) {
                CMD_ACK -> {
                    Log.d("ProtocolHandler", "ACK received. Sending next command.")
                    // The ACK is a response from Arduino, it does not mean a command sent by Android is finished.
                    // The onCommandSent() is called by BluetoothService's onCharacteristicWrite.
                    return
                }
                LedControllerCommands.CMD_GET_LED_COUNT.toByte() -> {
                    if (bytes.size >= 3) {
                        val ledCount = (bytes[1].toInt() and 0xFF shl 8) or (bytes[2].toInt() and 0xFF)
                        Handler(Looper.getMainLooper()).post {
                            liveLedCount.value = ledCount
                        }
                        Log.d("ProtocolHandler", "Received LED Count: $ledCount")
                        // No onCommandSent() here, as it's a response, not a confirmation of a sent command.
                    } else {
                        Log.e("ProtocolHandler", "Malformed LED Count response: ${bytes.joinToString()}")
                    }
                    return
                }
                0.toByte() -> {
                    Log.d("ProtocolHandler", "Heartbeat or unexpected single byte received and discarded.")
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
                        val jsonElement = JsonParser.parseString(jsonString)
                        if (jsonElement.isJsonObject) {
                            val jsonObject = jsonElement.asJsonObject
                            if (jsonObject.has("effect") && jsonObject.has("params")) {
                                val effectName = jsonObject["effect"].asString
                                val paramsJsonArray = jsonObject["params"].asJsonArray

                                val parametersMap = mutableMapOf<String, Any>()
                                paramsJsonArray.forEach { paramElement ->
                                    val paramObj = paramElement.asJsonObject
                                    val name = paramObj["name"].asString
                                    val type = paramObj["type"].asString
                                    val valueElement = paramObj["value"]

                                    val value: Any = when (type) {
                                        "integer" -> valueElement.asInt
                                        "float" -> valueElement.asFloat
                                        "color" -> valueElement.asInt // Arduino sends color as an int
                                        "boolean" -> valueElement.asBoolean
                                        else -> valueElement.toString()
                                    }

                                    parametersMap[name] = value
                                }

                                val updatedEffects = EffectsRepository.effects.value?.toMutableList() ?: mutableListOf()
                                val existingEffectIndex = updatedEffects.indexOfFirst { it.name == effectName }

                                if (existingEffectIndex != -1) {
                                    updatedEffects[existingEffectIndex] = Effect(effectName, parametersMap)
                                } else {
                                    updatedEffects.add(Effect(effectName, parametersMap))
                                }

                                Handler(Looper.getMainLooper()).post {
                                    EffectsRepository.updateEffects(updatedEffects)
                                }
                                Log.d("ProtocolHandler", "Successfully parsed and updated effect parameters for $effectName.")
                                // No onCommandSent() here, as it's a response, not a confirmation of a sent command.

                            } else if (jsonObject.has("segments")) {
                                val status = gson.fromJson(jsonString, Status::class.java)

                                val effectObjects = status.effectNames.map { name ->
                                    Effect(name = name)
                                }

                                Handler(Looper.getMainLooper()).post {
                                    EffectsRepository.updateEffects(effectObjects)
                                    SegmentsRepository.updateSegments(status.segments)
                                }
                                Log.d("ProtocolHandler", "Successfully parsed and updated status.")

                                // Request parameters for all effects after initial status is received
                                status.effectNames.forEach { effectName ->
                                    requestEffectParameters(effectName)
                                }
                                // No onCommandSent() here, as it's a response, not a confirmation of a sent command.

                            } else {
                                Log.e("ProtocolHandler", "Unrecognized JSON format: $jsonString")
                            }
                        } else {
                            Log.e("ProtocolHandler", "Received non-object JSON: $jsonString")
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.e("ProtocolHandler", "Failed to parse JSON: ${e.message}. Raw JSON: $jsonString")
                    } finally {
                        responseBuffer.clear()
                        openBraceCount = 0
                    }
                }
            }
        }
        if (openBraceCount > 0) {
            Log.d("ProtocolHandler", "Partial message received. Buffer content: $responseBuffer")
        }
    }
}