package com.example.android_rave_controller.models

import androidx.lifecycle.ViewModel

class EffectsViewModel : ViewModel() {
    val effects = EffectsRepository.effects
}