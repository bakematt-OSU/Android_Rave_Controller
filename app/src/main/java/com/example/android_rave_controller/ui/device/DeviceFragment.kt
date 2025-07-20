package com.example.android_rave_controller.ui.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.arduino_comm_ble.control.CommandSetters
import com.example.android_rave_controller.arduino_comm_ble.ConnectionViewModel
import com.example.android_rave_controller.databinding.FragmentDeviceBinding
import com.example.android_rave_controller.ui.device.DeviceViewModel

class DeviceFragment : Fragment() {

    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels()
    private val deviceViewModel: DeviceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            binding.textViewDeviceName.text = status.deviceName ?: "Not Connected"
            if (status.isConnected) {
                CommandGetters.requestLedCount()
            }
        }

        deviceViewModel.deviceProtocolHandler.liveLedCount.observe(viewLifecycleOwner) { count ->
            binding.textViewLedCount.text = count.toString()
        }

        binding.buttonSaveConfigToDevice.setOnClickListener {
            if (connectionViewModel.connectionStatus.value?.isConnected == true) {
                CommandSetters.saveConfigurationToDevice()
                Toast.makeText(context, "Saving configuration to device...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Not connected to a device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}