// src/main/java/models/Status.kt
package com.example.android_rave_controller.models

import com.google.gson.annotations.SerializedName

// Status now only cares about segments, effects list is enriched in DeviceProtocolHandler
data class Status(
    // The 'effects' field here can be removed or kept as a raw list of strings
    // as the full Effect objects with parameters will be populated separately.
    // Renamed to effectNames and made non-nullable with a default empty list.
    @SerializedName("effects") val effectNames: List<String> = emptyList(),
    @SerializedName("segments") val segments: List<Segment>
)