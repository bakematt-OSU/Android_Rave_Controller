package com.example.android_rave_controller.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.android_rave_controller.R
import com.example.android_rave_controller.SegmentConfigurationActivity
import com.example.android_rave_controller.databinding.FragmentDashboardBinding
import com.example.android_rave_controller.ui.bluetooth.BluetoothActivity

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val args: DashboardFragmentArgs by navArgs()

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

        binding.deviceNameText.text = "Connected to: ${args.deviceName}"

        binding.buttonToControls.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_dashboard_to_navigation_controls)
        }

        binding.buttonToBluetooth.setOnClickListener {
            val intent = Intent(activity, BluetoothActivity::class.java)
            startActivity(intent)
        }

        binding.buttonAddSegment.setOnClickListener {
            val intent = Intent(activity, SegmentConfigurationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}