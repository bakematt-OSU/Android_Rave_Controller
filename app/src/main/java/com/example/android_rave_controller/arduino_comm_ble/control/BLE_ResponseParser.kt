package com.example.android_rave_controller.arduino_comm_ble.control

import android.util.Log
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException // Import JsonSyntaxException

object BLE_ResponseParser {

    fun parseResponse(responseBuffer: StringBuilder, bytes: ByteArray, deviceProtocolHandler: DeviceProtocolHandler) {
        // --- ADD THIS BLOCK TO HANDLE ACKS FOR THE CONFIGURATION PUSH ---
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_ACK.toByte()) {
            deviceProtocolHandler.onAckReceived()
            return // Stop further processing for this ACK
        }
        // --- End of ADDED BLOCK ---


        // Handle the initial effect count message (This part is already correct)
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()) {
            if (bytes.size >= 3) {
                val effectCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                // Acknowledge to start receiving the effect info
                // ... existing code ...
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
            }
            return
        }

        // --- FIX: Add this block to handle the segment count ---
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_ALL_SEGMENT_CONFIGS.toByte()) {
            if (bytes.size >= 3) {
                val segmentCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                // If there are segments to receive, send an ACK to start the process.
                if (segmentCount > 0) {
                    BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
                }
            }
            return // Stop further processing for this message
        }
        // --- End of FIX ---

        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_LED_COUNT.toByte()) {
            if (bytes.size >= 3) {
                val ledCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                deviceProtocolHandler.liveLedCount.postValue(ledCount)
            }
            return
        }


        // Append incoming text to the buffer (this part is correct)
        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)

        // Delegate buffer processing to the parser
        JsonResponseParser.processBuffer(responseBuffer,
            onEffectsReceived = { effects ->
                val currentEffects = EffectsRepository.effects.value?.toMutableList() ?: mutableListOf()
                effects.forEach { effect ->
                    val existingIndex = currentEffects.indexOfFirst { it.name == effect.name }
                    if (existingIndex != -1) {
                        currentEffects[existingIndex] = effect
                    } else {
                        currentEffects.add(effect)
                    }
                }
                EffectsRepository.updateEffects(currentEffects)
                // Acknowledge to get the next effect
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
            },
            onStatusReceived = { status ->
                // This part of your logic might need adjustment based on your app's flow.
                // For now, it continues to request all effects after receiving a status.
                CommandGetters.requestAllEffects()
            },
            onSegmentReceived = { segment ->
                SegmentsRepository.addSegment(segment)
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK.toByte()))
            }
        )
    }
}

object JsonResponseParser {
    private val gson = Gson()

    fun processBuffer(
        buffer: StringBuilder,
        onEffectsReceived: (List<Effect>) -> Unit,
        onStatusReceived: (Status) -> Unit,
        onSegmentReceived: (Segment) -> Unit
    ) {
        while (buffer.isNotEmpty()) {
            var braceCount = 0
            var startIndex: Int
            var endIndex = -1

            startIndex = buffer.indexOf('{')
            if (startIndex == -1) {
                // No opening brace found, clear buffer and wait for more data.
                buffer.clear()
                return
            }

            if (startIndex > 0) {
                // Remove leading non-JSON characters
                buffer.delete(0, startIndex)
            }
            // Reset startIndex to 0 after deleting leading characters.
            startIndex = 0

            for (i in startIndex until buffer.length) {
                if (buffer[i] == '{') {
                    braceCount++
                } else if (buffer[i] == '}') {
                    braceCount--
                    if (braceCount == 0) {
                        endIndex = i
                        break
                    }
                }
            }

            if (endIndex != -1) {
                val jsonString = buffer.substring(startIndex, endIndex + 1)
                try {
                    parse(jsonString, onEffectsReceived, onStatusReceived, onSegmentReceived)
                    // If parsing is successful, remove the parsed JSON string from the buffer
                    buffer.delete(0, endIndex + 1)
                } catch (e: JsonSyntaxException) { // Catch specific JSON parsing errors
                    Log.e("JsonResponseParser", "Malformed JSON detected, clearing buffer: $jsonString", e)
                    // If parsing fails due to malformed JSON, clear the entire buffer
                    // to prevent getting stuck on corrupted data.
                    buffer.clear()
                    return // Exit and wait for new, clean data
                } catch (e: Exception) { // Catch any other unexpected exceptions
                    Log.e("JsonResponseParser", "Unexpected error parsing JSON, clearing buffer: $jsonString", e)
                    buffer.clear()
                    return
                }
            } else {
                // No complete JSON object found yet, wait for more data.
                return
            }
        }
    }

    private fun parse(
        jsonString: String,
        onEffectsReceived: (List<Effect>) -> Unit,
        onStatusReceived: (Status) -> Unit,
        onSegmentReceived: (Segment) -> Unit
    ) {
        // This try-catch is now redundant due to the one in processBuffer, but keeping it
        // for defensive programming if parse is called directly elsewhere.
        try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            if (jsonObject.has("effect") && jsonObject.has("params")) {
                val effect = gson.fromJson(jsonObject, Effect::class.java)
                onEffectsReceived(listOf(effect))
            } else if (jsonObject.has("segments") && jsonObject.has("available_effects")) {
                val status = parseStatusFromJson(jsonObject)
                onStatusReceived(status)
            } else if (jsonObject.has("id") && jsonObject.has("startLed") && jsonObject.has("endLed")) {
                val segment = gson.fromJson(jsonString, Segment::class.java)
                onSegmentReceived(segment)
            }
        } catch (e: Exception) {
            // Log for debugging, but the outer catch in processBuffer will handle buffer clearing
            Log.e("JsonResponseParser", "Inner parse failed for: $jsonString", e)
            throw e // Re-throw to be caught by the outer block in processBuffer
        }
    }

    private fun parseStatusFromJson(jsonObject: JsonObject): Status {
        val effectNames = gson.fromJson(jsonObject.getAsJsonArray("available_effects"), Array<String>::class.java).toList()
        val jsonSegments = jsonObject.getAsJsonArray("segments")
        SegmentsRepository.clearAllSegments()
        val segments = jsonSegments.mapNotNull { jsonElement ->
            try {
                val segmentObject = jsonElement.asJsonObject
                val parametersObject = segmentObject.getAsJsonObject("parameters")
                val parametersMap = mutableMapOf<String, Any>()

                if (parametersObject != null) {
                    for ((key, valueElement) in parametersObject.entrySet()) {
                        val value = when {
                            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isBoolean -> valueElement.asBoolean
                            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isNumber -> valueElement.asDouble.toInt()
                            else -> gson.fromJson(valueElement, Any::class.java)
                        }
                        parametersMap[key] = value
                    }
                }

                Segment(
                    id = segmentObject.get("id")?.asInt ?: -1,
                    name = segmentObject.get("name")?.asString ?: "Unnamed Segment",
                    startLed = segmentObject.get("startLed")?.asInt ?: 0,
                    endLed = segmentObject.get("endLed")?.asInt ?: 0,
                    effect = segmentObject.get("effect")?.asString ?: "SolidColor",
                    brightness = segmentObject.get("brightness")?.asInt ?: 128,
                    parameters = parametersMap
                ).takeIf { it.id != -1 }
            } catch (e: Exception) {
                Log.e("JsonResponseParser", "Failed to parse a segment, skipping.", e)
                null
            }
        }
        SegmentsRepository.updateSegments(segments)
        return Status(effectNames = effectNames, segments = segments)
    }
}