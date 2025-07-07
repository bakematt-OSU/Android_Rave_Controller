package com.example.android_rave_controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.android_rave_controller.databinding.ListItemSegmentBinding
import com.example.android_rave_controller.models.Segment

class SegmentsAdapter(private var segments: MutableList<Segment>) :
    RecyclerView.Adapter<SegmentsAdapter.SegmentViewHolder>() {

    class SegmentViewHolder(private val binding: ListItemSegmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(segment: Segment) {
            binding.segmentNameTextView.text = segment.name
            binding.segmentDetailsTextView.text =
                "LEDs: ${segment.startLed} - ${segment.endLed} | Effect: ${segment.effect}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SegmentViewHolder {
        val binding = ListItemSegmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SegmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SegmentViewHolder, position: Int) {
        holder.bind(segments[position])
    }

    override fun getItemCount(): Int {
        return segments.size
    }

    fun updateSegments(newSegments: List<Segment>) {
        segments.clear()
        segments.addAll(newSegments)
        notifyDataSetChanged()
    }
}