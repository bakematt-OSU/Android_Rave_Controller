package com.example.android_rave_controller.ui.controls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.DeviceProtocolHandler // Import the handler
import com.example.android_rave_controller.databinding.FragmentControlsBinding

class ControlsFragment : Fragment() {

    private var _binding: FragmentControlsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentControlsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Update buttons to send binary commands
        binding.buttonClearSegments.setOnClickListener {
            BluetoothService.sendCommand(byteArrayOf(0x06)) // CMD_CLEAR_SEGMENTS
        }
        binding.buttonAddSegment.setOnClickListener {
            // Example: Add segment 0 from LED 0 to 50
            DeviceProtocolHandler.setSegmentRange(0, 0, 50)
        }
        binding.buttonSelectSegment.setOnClickListener {
            // Example: Select segment 1
            DeviceProtocolHandler.selectSegment(1)
        }
        binding.buttonRainbowCycle.setOnClickListener {
            // Example: Set effect to "Rainbow" (assuming it's index 1)
            DeviceProtocolHandler.setEffect(1)
        }
        binding.buttonSolidColor.setOnClickListener {
            // Example: Set effect to "Solid" (assuming it's index 0)
            DeviceProtocolHandler.setEffect(0)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}