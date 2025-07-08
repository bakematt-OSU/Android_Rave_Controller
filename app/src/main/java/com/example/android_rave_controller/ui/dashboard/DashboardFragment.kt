package com.example.android_rave_controller.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.android_rave_controller.ConnectionViewModel
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.FragmentDashboardBinding
import com.example.android_rave_controller.ui.bluetooth.BluetoothDialogFragment

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectionViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            binding.buttonToBluetooth.isEnabled = !isConnected
        }

        binding.buttonToControls.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_dashboard_to_navigation_controls)
        }

        binding.buttonToBluetooth.setOnClickListener {
            BluetoothDialogFragment().show(parentFragmentManager, "BluetoothDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}