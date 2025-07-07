// In app/src/main/java/com/example/android_rave_controller/models/Segment.kt

package com.example.android_rave_controller.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Segment(
    val id: String,
    var name: String,
    var startLed: Int,
    var endLed: Int,
    var effect: String,
    var brightness: Int = 255 // Default brightness to max
) : Parcelable