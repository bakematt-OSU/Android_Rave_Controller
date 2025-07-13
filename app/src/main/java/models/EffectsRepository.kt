package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData

data class Effect(
    val name: String,
    val parameters: Map<String, Any> = emptyMap()
)

object EffectsRepository {
    val effects = MutableLiveData<List<Effect>>(emptyList())

    // New function to update from a list
    fun updateEffects(newEffects: List<Effect>) {
        effects.postValue(newEffects)
    }
}