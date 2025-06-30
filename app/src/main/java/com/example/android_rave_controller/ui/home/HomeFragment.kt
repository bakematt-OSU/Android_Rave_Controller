package com.example.android_rave_controller.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.buttonClearSegments.setOnClickListener {
            BluetoothService.sendCommand("clearsegments")
        }
        binding.buttonAddSegment.setOnClickListener {
            BluetoothService.sendCommand("addsegment 0 50")
        }
        binding.buttonSelectSegment.setOnClickListener {
            BluetoothService.sendCommand("select 1")
        }
        binding.buttonRainbowCycle.setOnClickListener {
            BluetoothService.sendCommand("seteffect rainbow")
        }
        binding.buttonSolidColor.setOnClickListener {
            BluetoothService.sendCommand("seteffect solid")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}