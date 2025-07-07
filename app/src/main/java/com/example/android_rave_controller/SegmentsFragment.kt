package com.example.android_rave_controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android_rave_controller.databinding.FragmentSegmentsBinding
import com.example.android_rave_controller.models.Segment

class SegmentsFragment : Fragment() {

    private var _binding: FragmentSegmentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSegmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The programmatic padding fix has been removed as it's no longer needed.

        // 1. Create some sample data.
        val segmentList = listOf(
            Segment(id = "1", name = "Main Strip", startLed = 0, endLed = 59, effect = "Rainbow"),
            Segment(id = "2", name = "Bass Beat", startLed = 60, endLed = 89, effect = "Pulse"),
            Segment(id = "3", name = "Ambient", startLed = 90, endLed = 119, effect = "Solid Color")
        )

        // 2. Create an instance of your adapter.
        val segmentsAdapter = SegmentsAdapter(segmentList)

        // 3. Connect the adapter to the RecyclerView.
        binding.segmentsRecyclerView.adapter = segmentsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}