// src/main/java/com/example/android_rave_controller/arduino_comm_ble/DeviceProtocolHandler.kt
package com.example.android_rave_controller.arduino_comm_ble

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentsRepository
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue

object DeviceProtocolHandler {

    // region Command Constants
    private val CMD_GET_STATUS: Byte = LedControllerCommands.CMD_GET_STATUS.toByte()
    private val CMD_ACK: Byte = LedControllerCommands.CMD_ACK.toByte()
    private val CMD_GET_LED_COUNT: Byte = LedControllerCommands.CMD_GET_LED_COUNT.toByte()
    private val CMD_SET_LED_COUNT: Byte = LedControllerCommands.CMD_SET_LED_COUNT.toByte()
    private val CMD_GET_ALL_EFFECTS: Byte = LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()
    private val CMD_SET_ALL_SEGMENT_CONFIGS: Byte = LedControllerCommands.CMD_SET_ALL_SEGMENT_CONFIGS.toByte()
    private val CMD_SET_SINGLE_SEGMENT_JSON: Byte = LedControllerCommands.CMD_SET_SINGLE_SEGMENT_JSON.toByte()
    private val CMD_SET_EFFECT_PARAMETER: Byte = LedControllerCommands.CMD_SET_EFFECT_PARAMETER.toByte()
    // endregion

    private enum class ProtocolState {
        IDLE,
        WAITING_FOR_EFFECT_COUNT,
        WAITING_FOR_EFFECT_JSON
    }

    private var currentState = ProtocolState.IDLE
    private var expectedEffectCount = 0
    private var receivedEffectCount = 0

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

    // region Public Functions to Send Commands
    fun requestDeviceStatus() {
        currentState = ProtocolState.IDLE
        responseBuffer.clear()
        openBraceCount = 0
        queueCommand(byteArrayOf(CMD_GET_STATUS))
    }

    private fun requestAllEffects() {
        Log.d("ProtocolHandler", "Requesting all effects parameters...")
        currentState = ProtocolState.WAITING_FOR_EFFECT_COUNT
        queueCommand(byteArrayOf(CMD_GET_ALL_EFFECTS))
    }

    fun sendSingleSegment(segment: Segment) {
        val jsonPayload = gson.toJson(segment)
        val command = byteArrayOf(CMD_SET_SINGLE_SEGMENT_JSON) + jsonPayload.toByteArray(Charsets.UTF_8)
        queueCommand(command)
    }

    fun sendFullConfiguration(segments: List<Segment>) {
        commandQueue.clear()
        queueCommand(byteArrayOf(CMD_SET_ALL_SEGMENT_CONFIGS))
        val segmentCount = segments.size
        val countBytes = byteArrayOf((segmentCount shr 8).toByte(), (segmentCount and 0xFF).toByte())
        queueCommand(countBytes)
        segments.forEach { segment ->
            queueCommand(gson.toJson(segment).toByteArray(Charsets.UTF_8))
        }
    }

    // CORRECTION: Changed segmentId from String to Int
    fun sendParameterUpdate(segmentId: Int, paramName: String, paramType: String, value: Any) {
        val segIndex = SegmentsRepository.segments.value?.indexOfFirst { it.id == segmentId } ?: -1
        if (segIndex == -1) return

        val paramTypeOrdinal = when (paramType) {
            "integer" -> 0; "float" -> 1; "color" -> 2; "boolean" -> 3
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
        queueCommand(byteArrayOf(CMD_SET_EFFECT_PARAMETER) + commandPayload)
    }

    fun requestLedCount() {
        queueCommand(byteArrayOf(CMD_GET_LED_COUNT))
    }

    fun setLedCount(count: Int) {
        val countBytes = byteArrayOf((count shr 8).toByte(), (count and 0xFF).toByte())
        queueCommand(byteArrayOf(CMD_SET_LED_COUNT) + countBytes)
    }
    // endregion

    // region Command Queue & Response Parsing
    private fun queueCommand(command: ByteArray) {
        commandQueue.add(command)
        if (!isSendingCommand) sendNextCommandFromQueue()
    }

    private fun sendNextCommandFromQueue() {
        if (commandQueue.isEmpty() || isSendingCommand) return
        isSendingCommand = true
        commandQueue.poll()?.let { BluetoothService.sendCommand(it) }
    }

    fun onCommandSent() {
        isSendingCommand = false
        sendNextCommandFromQueue()
    }

    fun parseResponse(bytes: ByteArray) {
        if (bytes.isEmpty()) return

        when (bytes[0]) {
            CMD_GET_ALL_EFFECTS -> {
                if (bytes.size >= 3 && currentState == ProtocolState.WAITING_FOR_EFFECT_COUNT) {
                    expectedEffectCount = (bytes[1].toInt() and 0xFF shl 8) or (bytes[2].toInt() and 0xFF)
                    receivedEffectCount = 0
                    Log.d("ProtocolHandler", "Expecting $expectedEffectCount effects. Sending ACK to start.")
                    currentState = ProtocolState.WAITING_FOR_EFFECT_JSON
                    queueCommand(byteArrayOf(CMD_ACK))
                }
                return
            }
            CMD_GET_LED_COUNT -> {
                if (bytes.size >= 3) {
                    liveLedCount.postValue((bytes[1].toInt() and 0xFF shl 8) or (bytes[2].toInt() and 0xFF))
                }
                return
            }
            CMD_ACK -> { /* ACK is handled implicitly by the command queue */ return }
        }

        val incomingText = bytes.toString(Charsets.UTF_8)
        for (char in incomingText) {
            if (char == '{') {
                if (openBraceCount == 0) responseBuffer.clear()
                openBraceCount++
            }
            if (openBraceCount > 0) responseBuffer.append(char)
            if (char == '}') {
                openBraceCount--
                if (openBraceCount == 0 && responseBuffer.isNotEmpty()) {
                    processCompleteJson(responseBuffer.toString())
                    responseBuffer.clear()
                }
            }
        }
    }

    private fun processCompleteJson(jsonString: String) {
        try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            if (jsonObject.has("effect") && jsonObject.has("params")) {
                val effect = gson.fromJson(jsonObject, Effect::class.java)
                val currentEffects = EffectsRepository.effects.value?.toMutableList() ?: mutableListOf()
                val existingIndex = currentEffects.indexOfFirst { it.name == effect.name }
                if (existingIndex != -1) {
                    currentEffects[existingIndex] = effect
                    EffectsRepository.updateEffects(currentEffects)
                }
                receivedEffectCount++
                Log.d("ProtocolHandler", "Effect '${effect.name}' params received ($receivedEffectCount/$expectedEffectCount).")
                if (receivedEffectCount < expectedEffectCount) {
                    queueCommand(byteArrayOf(CMD_ACK))
                } else {
                    currentState = ProtocolState.IDLE
                    Log.d("ProtocolHandler", "All effect parameters downloaded.")
                }
            } else if (jsonObject.has("segments") && jsonObject.has("available_effects")) {
                // *** This is the fix ***
                // Manually parse the Status object to ensure correct types for parameters
                val status = parseStatusFromJson(jsonObject)

                SegmentsRepository.updateSegments(status.segments)
                val initialEffects = status.effectNames.map { Effect(name = it, parameters = emptyList()) }
                EffectsRepository.updateEffects(initialEffects)
                Log.d("ProtocolHandler", "Status processed. Now fetching all effect parameters.")
                requestAllEffects()
            }
        } catch (e: Exception) {
            Log.e("ProtocolHandler", "Failed to parse JSON: $jsonString", e)
            currentState = ProtocolState.IDLE
        }
    }

    // New function to manually parse the Status object from a JsonObject
    private fun parseStatusFromJson(jsonObject: JsonObject): Status {
        val effectNames = gson.fromJson(jsonObject.getAsJsonArray("available_effects"), Array<String>::class.java).toList()
        val jsonSegments = jsonObject.getAsJsonArray("segments")
        val segments = jsonSegments.map { jsonElement ->
            val segmentObject = jsonElement.asJsonObject
            val parametersObject = segmentObject.getAsJsonObject("parameters")
            val parametersMap = mutableMapOf<String, Any>()

            if (parametersObject != null) {
                for ((key, valueElement) in parametersObject.entrySet()) {
                    val value = when {
                        valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isBoolean -> valueElement.asBoolean
                        valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isNumber -> {
                            // This is the key change: ensure all numbers are stored as Int
                            valueElement.asDouble.toInt()
                        }
                        else -> gson.fromJson(valueElement, Any::class.java) // Fallback for other types
                    }
                    parametersMap[key] = value
                }
            }

            // Build the segment manually
            Segment(
                // CORRECTION: Changed from asString to asInt
                id = segmentObject.get("id").asInt,
                name = segmentObject.get("name").asString,
                startLed = segmentObject.get("startLed").asInt,
                endLed = segmentObject.get("endLed").asInt,
                effect = segmentObject.get("effect").asString,
                brightness = segmentObject.get("brightness").asInt,
                parameters = parametersMap
            )
        }
        return Status(effectNames = effectNames, segments = segments)
    }
    // endregion
}