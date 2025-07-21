package com.example.android_rave_controller.arduino_comm_ble.control

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.SegmentsRepository
import kotlin.text.iterator

class DeviceProtocolHandler(private val context: Context) {

    val liveLedCount = MutableLiveData<Int>()
    private val responseBuffer = StringBuilder()
    private lateinit var commandQueue: CommandQueue

    fun setCommandQueue(queue: CommandQueue) {
        this.commandQueue = queue
    }

    fun onCommandSent() {
        commandQueue.onCommandSent()
    }

    fun parseResponse(bytes: ByteArray) {
        BLE_ResponseParser.parseResponse(responseBuffer, bytes, this)
    }
}