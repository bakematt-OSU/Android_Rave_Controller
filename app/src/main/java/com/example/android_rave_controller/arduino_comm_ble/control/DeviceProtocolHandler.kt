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

    // Define the maximum chunk size for BLE packets (typically 20 bytes for most BLE modules)
    private val CHUNK_SIZE = 20

    // --- State Machine for sending full configuration ---
    private enum class ConfigState {
        IDLE,
        WAITING_FOR_START_ACK,
        SENDING_SEGMENT_COUNT,
        WAITING_FOR_COUNT_ACK,
        SENDING_SEGMENTS, // This state now means we are sending chunks of a segment
        WAITING_FOR_SEGMENT_ACK
    }

    private var configState = ConfigState.IDLE
    private var segmentsToSend: List<Segment> = emptyList()
    private var currentSegmentIndex = 0
    private val gson = Gson()

    // Variables for chunking the current segment's JSON payload
    private var currentSegmentJsonPayload: ByteArray = byteArrayOf()
    private var currentChunkOffset = 0
    private var totalChunksForCurrentSegment = 0
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

    /**
     * Initiates the process of sending the full configuration (all segments) to the Arduino.
     * This uses a state machine to handle acknowledgments and chunking.
     * @param segments The list of Segment objects to send.
     */
    fun startSendingFullConfiguration(segments: List<Segment>) {
        if (configState != ConfigState.IDLE) {
            // Already sending, ignore request
            return
        }
        segmentsToSend = segments
        currentSegmentIndex = 0
        currentChunkOffset = 0 // Reset chunk offset for new transfer
        totalChunksForCurrentSegment = 0 // Reset total chunks

        configState = ConfigState.WAITING_FOR_START_ACK
        // Send the command to initiate receiving all segment configs
        BluetoothService.sendCommand(byteArrayOf(LedControllerCommands.CMD_SET_ALL_SEGMENT_CONFIGS.toByte()))
    }

    /**
     * Advances the state machine for sending the full configuration.
     * This is called by `BLE_ResponseParser` when an ACK is received from the Arduino.
     */
    fun onAckReceived() {
        when (configState) {
            ConfigState.WAITING_FOR_START_ACK -> {
                // Arduino acknowledged the initiation command. Now send the total segment count.
                configState = ConfigState.WAITING_FOR_COUNT_ACK
                val segmentCount = segmentsToSend.size
                // Convert segmentCount to a 2-byte array (Big-endian)
                val countBytes = byteArrayOf((segmentCount shr 8).toByte(), (segmentCount and 0xFF).toByte())
                BluetoothService.sendCommand(countBytes)
            }
            ConfigState.WAITING_FOR_COUNT_ACK -> {
                // Arduino acknowledged the segment count. Now start sending segments.
                configState = ConfigState.SENDING_SEGMENTS
                sendNextSegment()
            }
            ConfigState.SENDING_SEGMENTS -> {
                // Arduino acknowledged a chunk of the current segment. Send the next chunk.
                currentChunkOffset += CHUNK_SIZE
                if (currentChunkOffset < currentSegmentJsonPayload.size) {
                    // More chunks for the current segment
                    sendCurrentSegmentChunk()
                } else {
                    // All chunks for the current segment sent. Move to the next segment.
                    currentSegmentIndex++
                    currentChunkOffset = 0 // Reset for the next segment
                    if (currentSegmentIndex < segmentsToSend.size) {
                        sendNextSegment() // Send the next segment
                    } else {
                        // All segments and their chunks have been sent.
                        configState = ConfigState.IDLE
                    }
                }
            }
            else -> {
                // Ignore unexpected ACKs in other states
            }
        }
    }

    /**
     * Prepares the JSON payload for the next segment and initiates sending its first chunk.
     */
    private fun sendNextSegment() {
        if (currentSegmentIndex < segmentsToSend.size) {
            val segment = segmentsToSend[currentSegmentIndex]
            // Convert the Segment object to JSON string and then to ByteArray
            currentSegmentJsonPayload = gson.toJson(segment).toByteArray(Charsets.UTF_8)
            currentChunkOffset = 0 // Start from the beginning of this segment's payload
            totalChunksForCurrentSegment = (currentSegmentJsonPayload.size + CHUNK_SIZE - 1) / CHUNK_SIZE // Calculate total chunks

            sendCurrentSegmentChunk() // Send the first chunk of this segment
        }
    }

    /**
     * Sends the current 20-byte chunk of the `currentSegmentJsonPayload`.
     */
    private fun sendCurrentSegmentChunk() {
        val remainingBytes = currentSegmentJsonPayload.size - currentChunkOffset
        val chunkSizeToSend = minOf(CHUNK_SIZE, remainingBytes)

        if (chunkSizeToSend > 0) {
            val chunk = currentSegmentJsonPayload.copyOfRange(
                currentChunkOffset,
                currentChunkOffset + chunkSizeToSend
            )
            BluetoothService.sendCommand(chunk)
        }
    }
}
