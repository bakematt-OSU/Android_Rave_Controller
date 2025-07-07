package com.example.android_rave_controller.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.ConnectionViewModel
import com.example.android_rave_controller.databinding.FragmentHomeBinding
import com.example.android_rave_controller.ui.bluetooth.BluetoothDialogFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.buttonToBluetooth.setOnClickListener {
            BluetoothDialogFragment().show(parentFragmentManager, "BluetoothDialog")
        }

        BluetoothService.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                val deviceName = BluetoothService.connectedDevice?.name ?: "Unknown Device"
                connectionViewModel.updateConnection(true, deviceName)
            } else {
                connectionViewModel.updateConnection(false, null)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
