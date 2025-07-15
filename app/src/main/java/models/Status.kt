// src/main/java/models/Status.kt
package com.example.android_rave_controller.models

import com.google.gson.annotations.SerializedName

// Status now only cares about segments, effects list is enriched in DeviceProtocolHandler
data class Status(
    // CORRECTED: Changed @SerializedName from "effects" to "available_effects"
    @SerializedName("available_effects") val effectNames: List<String> = emptyList(),
    @SerializedName("segments") val segments: List<Segment>
)
