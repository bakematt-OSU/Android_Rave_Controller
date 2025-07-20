package com.example.android_rave_controller.arduino_comm_ble

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.models.EffectsRepository

object DeviceProtocolHandler {

    val liveLedCount = MutableLiveData<Int>()
    private lateinit var applicationContext: Context
    private val responseBuffer = StringBuilder()
    private var openBraceCount = 0

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun onCommandSent() {
        CommandQueue.onCommandSent()
    }

    fun parseResponse(bytes: ByteArray) {
        val incomingText = bytes.toString(Charsets.UTF_8)
        for (char in incomingText) {
            if (char == '{') {
                if (openBraceCount == 0) responseBuffer.clear()
                openBraceCount++
            }
            if (openBraceCount > 0) responseBuffer.append(char)
            if (char == '}') {
                openBraceCount--
                if (openBraceCount == 0 && responseBuffer.isNotEmpty()) {
                    JsonResponseParser.parse(responseBuffer.toString(),
                        onEffectsReceived = { effects ->
                            val currentEffects = EffectsRepository.effects.value?.toMutableList() ?: mutableListOf()
                            effects.forEach { effect ->
                                val existingIndex = currentEffects.indexOfFirst { it.name == effect.name }
                                if (existingIndex != -1) {
                                    currentEffects[existingIndex] = effect
                                } else {
                                    currentEffects.add(effect)
                                }
                            }
                            EffectsRepository.updateEffects(currentEffects)
                        },
                        onStatusReceived = { status ->
                            CommandGetters.requestAllEffects()
                        }
                    )
                    responseBuffer.clear()
                }
            }
        }
    }
}