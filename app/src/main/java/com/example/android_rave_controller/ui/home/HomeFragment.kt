package com.example.android_rave_controller.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.FragmentHomeBinding
import com.example.android_rave_controller.ui.bluetooth.BluetoothActivity

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

        binding.buttonToBluetooth.setOnClickListener {
            val intent = Intent(activity, BluetoothActivity::class.java)
            startActivity(intent)
        }

        // Observe connection state
        BluetoothService.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                binding.textConnectionStatus.text = "Status: Connected"
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_200))
            } else {
                binding.textConnectionStatus.text = "Status: Disconnected"
                binding.textConnectionStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}