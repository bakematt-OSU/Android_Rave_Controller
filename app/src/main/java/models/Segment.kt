// src/main/java/com/example/android_rave_controller/models/Segment.kt
package com.example.android_rave_controller.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Segment(
    val id: String,
    var name: String,
    var startLed: Int,
    var endLed: Int,
    var effect: String,
    var brightness: Int = 255,
    // Add @RawValue to tell Parcelize how to handle the generic Map
    var parameters: @RawValue Map<String, Any>? = emptyMap()
) : Parcelable