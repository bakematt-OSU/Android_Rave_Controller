package com.example.android_rave_controller.models

import com.google.gson.annotations.SerializedName

// This class now handles a potentially missing "effects" list from the JSON
data class Status(
    @SerializedName("effects") val effects: List<String>?, // Made nullable to prevent parsing errors
    @SerializedName("segments") val segments: List<Segment>
)