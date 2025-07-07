package com.example.android_rave_controller.models

import com.google.gson.annotations.SerializedName

// This class perfectly matches the new JSON from the Arduino
data class Status(
    @SerializedName("effects") val effects: List<String>,
    @SerializedName("segments") val segments: List<Segment>
)