package com.example.android_rave_controller.models

import androidx.lifecycle.ViewModel

class SegmentViewModel : ViewModel() {
    val segments = SegmentsRepository.segments

    fun addSegment(segment: Segment) {
        SegmentsRepository.addSegment(segment)
    }
}