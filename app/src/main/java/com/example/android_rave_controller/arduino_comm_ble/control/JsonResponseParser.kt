package com.example.android_rave_controller.arduino_comm_ble.control

import android.util.Log
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.Status
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
                buffer.clear()
                return
            }

            if (startIndex > 0) {
                buffer.delete(0, startIndex)
            }
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
                parse(jsonString, onEffectsReceived, onStatusReceived, onSegmentReceived)

                buffer.delete(0, endIndex + 1)
            } else {
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
            Log.e("JsonResponseParser", "Failed to parse JSON: $jsonString", e)
        }
    }

    private fun parseStatusFromJson(jsonObject: JsonObject): Status {
        val effectNames = gson.fromJson(jsonObject.getAsJsonArray("available_effects"), Array<String>::class.java).toList()
        val jsonSegments = jsonObject.getAsJsonArray("segments")
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
        return Status(effectNames = effectNames, segments = segments)
    }
}