package com.example.android_rave_controller.models

// This is the main configuration object that will be sent
data class RaveConfiguration(
    val segments: List<Segment>,
    val effects: List<String>
)

// This is a temporary object used for creating the JSON,
// where the effect is an index instead of a name.
data class RaveConfigurationForTransport(
    val segments: List<SegmentForTransport>,
    val effects: List<String>
)

data class SegmentForTransport(
    val id: String,
    var name: String,
    var startLed: Int,
    var endLed: Int,
    var effectIndex: Int, // The effect is now an index (Int)
    var brightness: Int
)