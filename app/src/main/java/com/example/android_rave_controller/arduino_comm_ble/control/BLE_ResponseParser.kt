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
        // Handle ACKs for the configuration push process
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_ACK_GENERIC.toByte()) {
            Log.d("BLE_ResponseParser", "Received ACK_GENERIC.")
            deviceProtocolHandler.onAckReceived()
            return // Stop further processing for this ACK
        }

        // Handle the initial effect count message
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_ALL_EFFECTS.toByte()) {
            if (bytes.size >= 3) {
                val effectCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                Log.d("BLE_ResponseParser", "Received effect count: $effectCount")
                // Acknowledge to start receiving the effect info
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK_GENERIC.toByte()))
            }
            return
        }

        // Handle the segment count message
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_ALL_SEGMENT_CONFIGS.toByte()) {
            if (bytes.size >= 3) {
                val segmentCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                Log.d("BLE_ResponseParser", "Received segment count: $segmentCount")
                // If there are segments to receive, send an ACK to start the process.
                if (segmentCount > 0) {
                    BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK_GENERIC.toByte()))
                }
            }
            return // Stop further processing for this message
        }

        // Handle LED count message
        if (bytes.isNotEmpty() && bytes[0] == LedControllerCommands.CMD_GET_LED_COUNT.toByte()) {
            if (bytes.size >= 3) {
                val ledCount = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
                deviceProtocolHandler.liveLedCount.postValue(ledCount)
                Log.d("BLE_ResponseParser", "Received LED count: $ledCount")
            }
            return
        }

        // Append incoming text to the buffer
        val incomingText = bytes.toString(Charsets.UTF_8)
        responseBuffer.append(incomingText)
        Log.d("BLE_ResponseParser", "Appended to buffer. Current buffer: ${responseBuffer.length} bytes")

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
                Log.d("BLE_ResponseParser", "Processed effect JSON. Sending ACK_GENERIC.")
                // Acknowledge to get the next effect
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK_GENERIC.toByte()))
            },
            onStatusReceived = { status ->
                // This part of your logic might need adjustment based on your app's flow.
                // For now, it continues to request all effects after receiving a status.
                Log.d("BLE_ResponseParser", "Processed status JSON. Requesting all effects.")
                CommandGetters.requestAllEffects()
            },
            onSegmentReceived = { segment ->
                SegmentsRepository.addSegment(segment)
                Log.d("BLE_ResponseParser", "Processed segment JSON. Sending ACK_GENERIC.")
                BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_ACK_GENERIC.toByte()))
            }
        )
    }
}

object JsonResponseParser {
    private val gson = Gson()

    /**
     * Processes the StringBuilder buffer, extracting and parsing complete JSON objects.
     * It handles partial JSON messages by waiting for more data.
     * @param buffer The StringBuilder containing potentially partial or multiple JSON strings.
     * @param onEffectsReceived Callback for when an Effect JSON is parsed.
     * @param onStatusReceived Callback for when a Status JSON is parsed.
     * @param onSegmentReceived Callback for when a Segment JSON is parsed.
     */
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
                Log.d("JsonResponseParser", "No opening brace found, clearing buffer.")
                buffer.clear()
                return
            }

            if (startIndex > 0) {
                // Remove leading non-JSON characters
                Log.w("JsonResponseParser", "Found leading non-JSON characters, removing: ${buffer.substring(0, startIndex)}")
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
                    Log.d("JsonResponseParser", "Successfully parsed and removed JSON. Remaining buffer size: ${buffer.length}")
                } catch (e: JsonSyntaxException) { // Catch specific JSON parsing errors
                    // MODIFIED: Do not clear the entire buffer on malformed JSON
                    Log.e("JsonResponseParser", "Malformed JSON detected for: '$jsonString'. Removing from buffer and attempting to continue.", e)
                    buffer.delete(0, endIndex + 1) // Remove only the part that caused the error
                    // Continue the loop to check for more JSONs in the remaining buffer
                } catch (e: Exception) { // Catch any other unexpected exceptions
                    Log.e("JsonResponseParser", "Unexpected error parsing JSON, clearing buffer: '$jsonString'", e)
                    buffer.clear()
                    return
                }
            } else {
                // No complete JSON object found yet, wait for more data.
                Log.d("JsonResponseParser", "Incomplete JSON object, waiting for more data.")
                return
            }
        }
    }

    /**
     * Parses a single complete JSON string and dispatches it to the appropriate callback.
     * @param jsonString The complete JSON string to parse.
     */
    private fun parse(
        jsonString: String,
        onEffectsReceived: (List<Effect>) -> Unit,
        onStatusReceived: (Status) -> Unit,
        onSegmentReceived: (Segment) -> Unit
    ) {
        try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            when {
                // Check for Effect JSON structure
                jsonObject.has("effect") && jsonObject.has("params") -> {
                    val effect = gson.fromJson(jsonObject, Effect::class.java)
                    onEffectsReceived(listOf(effect))
                }
                // Check for Status JSON structure (contains "segments" and "available_effects")
                jsonObject.has("segments") && jsonObject.has("available_effects") -> {
                    val status = parseStatusFromJson(jsonObject)
                    onStatusReceived(status)
                }
                // Check for single Segment JSON structure
                jsonObject.has("id") && jsonObject.has("name") && jsonObject.has("startLed") && jsonObject.has("endLed") -> {
                    val segment = gson.fromJson(jsonString, Segment::class.java)
                    onSegmentReceived(segment)
                }
                else -> {
                    Log.w("JsonResponseParser", "Unknown JSON structure received: $jsonString")
                }
            }
        } catch (e: Exception) {
            // Log for debugging, but the outer catch in processBuffer will handle buffer clearing
            Log.e("JsonResponseParser", "Inner parse failed for: '$jsonString'", e)
            throw e // Re-throw to be caught by the outer block in processBuffer
        }
    }

    /**
     * Parses the Status JSON object, specifically handling the nested parameters within segments.
     * @param jsonObject The JsonObject representing the full status.
     * @return A Status data class object.
     */
    private fun parseStatusFromJson(jsonObject: JsonObject): Status {
        val effectNames = gson.fromJson(jsonObject.getAsJsonArray("available_effects"), Array<String>::class.java).toList()
        val jsonSegments = jsonObject.getAsJsonArray("segments")
        SegmentsRepository.clearAllSegments() // Clear existing segments before updating

        val segments = jsonSegments.mapNotNull { jsonElement ->
            try {
                val segmentObject = jsonElement.asJsonObject
                val parametersObject = segmentObject.getAsJsonObject("parameters")
                val parametersMap = mutableMapOf<String, Any>()

                if (parametersObject != null) {
                    for ((key, valueElement) in parametersObject.entrySet()) {
                        val value = when {
                            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isBoolean -> valueElement.asBoolean
                            valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isNumber -> valueElement.asDouble.toInt() // Assuming integers for now
                            else -> gson.fromJson(valueElement, Any::class.java) // Fallback for other types
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
                ).takeIf { it.id != -1 } // Only return if ID is valid
            } catch (e: Exception) {
                Log.e("JsonResponseParser", "Failed to parse a segment from status, skipping.", e)
                null
            }
        }
        SegmentsRepository.updateSegments(segments) // Update the repository
        return Status(effectNames = effectNames, segments = segments)
    }
}