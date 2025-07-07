package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData

object SegmentsRepository {
    val segments = MutableLiveData<MutableList<Segment>>(mutableListOf())

    fun addSegment(segment: Segment) {
        val currentList = segments.value ?: mutableListOf()
        currentList.add(segment)
        segments.value = currentList
    }

    // New function to update from a list
    fun updateSegments(newSegments: List<Segment>) {
        segments.postValue(newSegments.toMutableList())
    }
}