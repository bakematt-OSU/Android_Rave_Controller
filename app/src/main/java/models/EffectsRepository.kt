package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData

object EffectsRepository {
    val effects = MutableLiveData<List<String>>(emptyList())

    // New function to update from a list
    fun updateEffects(newEffects: List<String>) {
        effects.postValue(newEffects)
    }
}