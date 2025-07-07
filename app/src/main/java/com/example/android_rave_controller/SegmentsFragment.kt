package com.example.android_rave_controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.databinding.FragmentSegmentsBinding
import com.example.android_rave_controller.models.SegmentViewModel

class SegmentsFragment : Fragment() {

    private var _binding: FragmentSegmentsBinding? = null
    private val binding get() = _binding!!
    private val segmentViewModel: SegmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSegmentsBinding.inflate(inflater, container, false) // Typo corrected here
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val segmentsAdapter = SegmentsAdapter(mutableListOf())
        binding.segmentsRecyclerView.adapter = segmentsAdapter

        segmentViewModel.segments.observe(viewLifecycleOwner) { segments ->
            segmentsAdapter.updateSegments(segments)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}