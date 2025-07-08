package com.example.android_rave_controller

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.databinding.FragmentSegmentsBinding
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.EffectsViewModel
import com.example.android_rave_controller.models.RaveConfiguration
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import com.example.android_rave_controller.models.SegmentsRepository

class SegmentsFragment : Fragment() {

    private var _binding: FragmentSegmentsBinding? = null
    private val binding get() = _binding!!
    private val segmentViewModel: SegmentViewModel by activityViewModels()
    private val effectsViewModel: EffectsViewModel by activityViewModels()
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSegmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val segmentsAdapter = SegmentsAdapter(mutableListOf()) { segment ->
            onSegmentClicked(segment)
        }
        binding.segmentsRecyclerView.adapter = segmentsAdapter

        segmentViewModel.segments.observe(viewLifecycleOwner) { segments ->
            segmentsAdapter.updateSegments(segments)
        }

        // Set up listeners for the new buttons
        binding.buttonPush.setOnClickListener { pushConfiguration() }
        binding.buttonAdd.setOnClickListener {
            val intent = Intent(activity, SegmentConfigurationActivity::class.java)
            startActivity(intent)
        }
        binding.buttonLoad.setOnClickListener { showLoadDialog() }
        binding.buttonSave.setOnClickListener { showSaveDialog() }
        binding.buttonGetConfig.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_get_config_title))
                .setMessage(getString(R.string.dialog_get_config_message))
                .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                    DeviceProtocolHandler.requestDeviceStatus()
                    Toast.makeText(context, "Requesting segment update...", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.dialog_no), null)
                .show()
        }
    }

    private fun onSegmentClicked(segment: Segment) {
        val intent = Intent(activity, SegmentConfigurationActivity::class.java)
        intent.putExtra("EXTRA_SEGMENT_TO_EDIT", segment)
        startActivity(intent)
    }

    private fun pushConfiguration() {
        if (connectionViewModel.isConnected.value != true) {
            Toast.makeText(context, "Please connect to a device first.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentSegments = segmentViewModel.segments.value
        val effectsList = effectsViewModel.effects.value

        if (currentSegments != null && effectsList != null) {
            DeviceProtocolHandler.sendFullConfiguration(currentSegments, effectsList)
            Toast.makeText(context, "Pushing configuration to device...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not push configuration. Data is missing.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSaveDialog() {
        val currentSegments = segmentViewModel.segments.value
        val currentEffects = effectsViewModel.effects.value
        if (currentSegments.isNullOrEmpty() || currentEffects.isNullOrEmpty()) {
            Toast.makeText(context, "No configuration to save.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = "Configuration Name"
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_save_config_title))
            .setView(input)
            .setPositiveButton(getString(R.string.button_save)) { _, _ ->
                val filename = input.text.toString()
                if (filename.isNotBlank()) {
                    val configToSave = RaveConfiguration(segments = currentSegments, effects = currentEffects)
                    ConfigurationManager.saveConfiguration(requireContext(), configToSave, filename)
                    Toast.makeText(context, "Configuration '$filename' saved.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Filename cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.button_cancel), null)
            .show()
    }

    private fun showLoadDialog() {
        val savedFiles = ConfigurationManager.getSavedConfigurations(requireContext())
        if (savedFiles.isEmpty()) {
            Toast.makeText(context, "No saved configurations found.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_load_config_select_title))
            .setItems(savedFiles) { _, which ->
                val filename = savedFiles[which]
                loadConfiguration(filename)
            }
            .show()
    }

    private fun loadConfiguration(filename: String) {
        val loadedConfig = ConfigurationManager.loadConfiguration(requireContext(), filename)
        if (loadedConfig != null) {
            // Update both repositories with the loaded data
            SegmentsRepository.updateSegments(loadedConfig.segments)
            EffectsRepository.updateEffects(loadedConfig.effects)

            if (connectionViewModel.isConnected.value == true) {
                pushConfiguration() // Re-use the new push function.
                Toast.makeText(context, "Configuration '$filename' loaded and sent to device.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Configuration '$filename' loaded locally. Connect and push to sync.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Failed to load '$filename'.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}