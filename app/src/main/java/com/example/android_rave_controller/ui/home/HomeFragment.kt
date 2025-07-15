package com.example.android_rave_controller.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.arduino_comm.bluetooth.ConnectionViewModel
import com.example.android_rave_controller.databinding.FragmentHomeBinding
import com.example.android_rave_controller.arduino_comm.bluetooth.BluetoothDialogFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // This ViewModel is shared with the MainActivity
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.buttonToBluetooth.setOnClickListener {
            // Shows the Bluetooth scanning dialog
            BluetoothDialogFragment().show(parentFragmentManager, "BluetoothDialog")
        }

        // The observer that was here has been removed, as the MainActivity
        // is already handling the connection status updates.

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}