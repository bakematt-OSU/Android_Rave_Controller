package com.example.android_rave_controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.android_rave_controller.databinding.ListItemSegmentBinding
import com.example.android_rave_controller.models.Segment

class SegmentsAdapter(private val segments: List<Segment>) :
    RecyclerView.Adapter<SegmentsAdapter.SegmentViewHolder>() {

    // This inner class holds the views for a single list item.
    // It uses the auto-generated 'ListItemSegmentBinding' class.
    class SegmentViewHolder(private val binding: ListItemSegmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(segment: Segment) {
            binding.segmentNameTextView.text = segment.name
            binding.segmentDetailsTextView.text =
                "LEDs: ${segment.startLed} - ${segment.endLed} | Effect: ${segment.effect}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SegmentViewHolder {
        // This creates a new view holder and its binding.
        val binding = ListItemSegmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SegmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SegmentViewHolder, position: Int) {
        // This connects the data from a specific segment to the view holder.
        holder.bind(segments[position])
    }

    override fun getItemCount(): Int {
        // This tells the RecyclerView how many items are in the list.
        return segments.size
    }
}