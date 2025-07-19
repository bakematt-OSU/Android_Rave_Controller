// src/main/java/com/example/android_rave_controller/models/SegmentViewModel.kt
package com.example.android_rave_controller.models

import androidx.lifecycle.ViewModel

class SegmentViewModel : ViewModel() {
    val segments = SegmentsRepository.segments

    fun addSegment(segment: Segment) {
        SegmentsRepository.addSegment(segment)
    }

    fun updateSegment(segment: Segment) {
        SegmentsRepository.updateSegment(segment)
    }

    fun deleteSegment(segmentId: Int) { // Changed from String to Int
        SegmentsRepository.deleteSegment(segmentId)
    }
}