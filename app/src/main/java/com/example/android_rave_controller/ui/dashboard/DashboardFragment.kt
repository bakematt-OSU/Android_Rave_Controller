package com.example.android_rave_controller.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    // This line should now resolve correctly
    private val args: DashboardFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.deviceNameText.text = "Connected to: ${args.deviceName}"

        binding.buttonToControls.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_dashboard_to_navigation_controls)
        }

        binding.buttonToBluetooth.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_bluetooth)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}