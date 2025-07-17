// src/main/java/com/example/android_rave_controller/models/EffectsRepository.kt
package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.SerializedName

/**
 * Represents a single effect and its parameters.
 * The @SerializedName annotations ensure correct mapping from the JSON
 * keys ("effect", "params") to the data class fields (name, parameters).
 */
data class Effect(
    @SerializedName("effect") val name: String,
    @SerializedName("params") val parameters: List<EffectParameter> = emptyList()
)

/**
 * Represents a single parameter within an effect.
 */
data class EffectParameter(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: Any,
    @SerializedName("min_val") val minVal: Float?,
    @SerializedName("max_val") val maxVal: Float?
)

object EffectsRepository {
    val effects = MutableLiveData<List<Effect>>(emptyList())

    fun updateEffects(newEffects: List<Effect>) {
        effects.postValue(newEffects)
    }
}