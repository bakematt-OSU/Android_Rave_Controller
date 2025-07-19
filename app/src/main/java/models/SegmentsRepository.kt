// src/main/java/com/example/android_rave_controller/models/SegmentsRepository.kt
package com.example.android_rave_controller.models

import androidx.lifecycle.MutableLiveData

object SegmentsRepository {
    val segments = MutableLiveData<MutableList<Segment>>(mutableListOf())

    fun addSegment(segment: Segment) {
        val currentList = segments.value ?: mutableListOf()
        currentList.add(segment)
        segments.postValue(currentList)
    }

    fun updateSegment(updatedSegment: Segment) {
        val currentList = segments.value ?: return
        val index = currentList.indexOfFirst { it.id == updatedSegment.id }
        if (index != -1) {
            currentList[index] = updatedSegment
            segments.postValue(currentList)
        }
    }

    fun deleteSegment(segmentId: Int) { // Changed from String to Int
        val currentList = segments.value ?: return
        currentList.removeAll { it.id == segmentId }
        segments.postValue(currentList)
    }

    fun updateSegments(newSegments: List<Segment>) {
        segments.postValue(newSegments.toMutableList())
    }
}