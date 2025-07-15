// In app/src/main/java/com/example/android_rave_controller/models/Parameter.kt
package com.example.android_rave_controller.models

data class Parameter<T>( // Generic type for value
    val name: String,
    val type: String, // "integer", "float", "color", "boolean"
    val value: T,
    val min: Float? = null, // Nullable for types like color/boolean
    val max: Float? = null  // Nullable for types like color/boolean
)

// Enum to map Arduino's ParamType integers to strings for clarity
enum class ParamType(val stringValue: String) {
    INTEGER("integer"),
    FLOAT("float"),
    COLOR("color"),
    BOOLEAN("boolean");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.ordinal == value }?.stringValue ?: "unknown"
    }
}