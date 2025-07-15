// src/main/java/models/EffectsRepository.kt
package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData

// Update the Effect data class to include parameters as a Map
data class Effect(
    val name: String,
    val parameters: Map<String, Any> = emptyMap() // Changed to Map<String, Any>
)

object EffectsRepository {
    val effects = MutableLiveData<List<Effect>>(emptyList())

    // New function to update from a list
    fun updateEffects(newEffects: List<Effect>) {
        effects.postValue(newEffects)
    }
}