package com.example.android_rave_controller.arduino_comm_ble.control

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentsRepository
import com.google.gson.Gson

class DeviceProtocolHandler(private val context: Context) {

    val liveLedCount = MutableLiveData<Int>()
    private val responseBuffer = StringBuilder()
    private lateinit var commandQueue: CommandQueue

    // --- State Machine for sending full configuration ---
    private enum class ConfigState {
        IDLE,
        WAITING_FOR_START_ACK,
        SENDING_SEGMENT_COUNT,
        WAITING_FOR_COUNT_ACK,
        SENDING_SEGMENTS,
        WAITING_FOR_SEGMENT_ACK
    }

    private var configState = ConfigState.IDLE
    private var segmentsToSend: List<Segment> = emptyList()
    private var currentSegmentIndex = 0
    private val gson = Gson()
    // --- End of State Machine ---


    fun setCommandQueue(queue: CommandQueue) {
        this.commandQueue = queue
    }

    fun onCommandSent() {
        commandQueue.onCommandSent()
    }

    fun parseResponse(bytes: ByteArray) {
        BLE_ResponseParser.parseResponse(responseBuffer, bytes, this)
    }

    // --- New method to start sending the configuration ---
    fun startSendingFullConfiguration(segments: List<Segment>) {
        if (configState != ConfigState.IDLE) {
            // Already sending, ignore request
            return
        }
        segmentsToSend = segments
        currentSegmentIndex = 0
        configState = ConfigState.WAITING_FOR_START_ACK
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SET_ALL_SEGMENT_CONFIGS.toByte()))
    }

    // --- New method to advance the state machine ---
    fun onAckReceived() {
        when (configState) {
            ConfigState.WAITING_FOR_START_ACK -> {
                configState = ConfigState.WAITING_FOR_COUNT_ACK
                val segmentCount = segmentsToSend.size
                val countBytes = byteArrayOf((segmentCount shr 8).toByte(), (segmentCount and 0xFF).toByte())
                BluetoothService.sendCommand(countBytes)
            }
            ConfigState.WAITING_FOR_COUNT_ACK -> {
                configState = ConfigState.WAITING_FOR_SEGMENT_ACK
                sendNextSegment()
            }
            ConfigState.WAITING_FOR_SEGMENT_ACK -> {
                if (currentSegmentIndex < segmentsToSend.size) {
                    sendNextSegment()
                } else {
                    // Finished sending all segments
                    configState = ConfigState.IDLE
                }
            }
            else -> {
                // Ignore unexpected ACKs
            }
        }
    }

    private fun sendNextSegment() {
        if (currentSegmentIndex < segmentsToSend.size) {
            val segment = segmentsToSend[currentSegmentIndex]
            val jsonPayload = gson.toJson(segment).toByteArray(Charsets.UTF_8)
            BluetoothService.sendCommand(jsonPayload)
            currentSegmentIndex++
        }
    }
}