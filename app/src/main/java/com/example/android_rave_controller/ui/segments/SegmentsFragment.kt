package com.example.android_rave_controller.ui.segments

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
import com.example.android_rave_controller.R
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.arduino_comm_ble.control.CommandSetters
import com.example.android_rave_controller.arduino_comm_ble.ConnectionViewModel
import com.example.android_rave_controller.databinding.FragmentSegmentsBinding
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.RaveConfiguration
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import com.example.android_rave_controller.support_code.ConfigurationManager

class SegmentsFragment : Fragment() {

    private var _binding: FragmentSegmentsBinding? = null
    private val binding get() = _binding!!
    private val segmentViewModel: SegmentViewModel by activityViewModels()
    private val connectionViewModel: ConnectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        binding.buttonPush.setOnClickListener { pushConfiguration() }
        binding.buttonAdd.setOnClickListener {
            startActivity(Intent(activity, SegmentConfigurationActivity::class.java))
        }
        binding.buttonLoad.setOnClickListener { showLoadDialog() }
        binding.buttonSave.setOnClickListener { showSaveDialog() }
        binding.buttonGetConfig.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                .setTitle(getString(R.string.dialog_get_config_title))
                .setMessage(getString(R.string.dialog_get_config_message))
                .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                    CommandGetters.requestDeviceStatus()
                    Toast.makeText(context, "Refreshing configuration…", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.dialog_no), null)
                .show()
        }
        binding.buttonWizard.setOnClickListener {
            SegmentsWizardDialogFragment().show(parentFragmentManager, "SegmentsWizardDialog")
        }
    }

    private fun onSegmentClicked(segment: Segment) {
        Intent(activity, SegmentConfigurationActivity::class.java).apply {
            putExtra("EXTRA_SEGMENT_TO_EDIT", segment)
            startActivity(this)
        }
    }

    private fun pushConfiguration() {
        if (connectionViewModel.isConnected.value != true) {
            Toast.makeText(context, "Please connect to a device first.", Toast.LENGTH_SHORT).show()
            return
        }
        segmentViewModel.segments.value?.let {
            if (it.isNotEmpty()) {
                CommandSetters.sendFullConfiguration(it)
                Toast.makeText(context, "Pushing configuration…", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSaveDialog() {
        val currentSegments = segmentViewModel.segments.value ?: return
        val currentEffects = EffectsRepository.effects.value?.map { it.name } ?: return

        val input = EditText(requireContext()).apply { hint = "Configuration name" }
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle(getString(R.string.dialog_save_config_title))
            .setView(input)
            .setPositiveButton(getString(R.string.button_save)) { _, _ ->
                val filename = input.text.toString().trim()
                if (filename.isNotEmpty()) {
                    val configToSave = RaveConfiguration(segments = currentSegments, effects = currentEffects)
                    ConfigurationManager.saveConfiguration(requireContext(), configToSave, filename)
                    Toast.makeText(context, "'$filename' saved.", Toast.LENGTH_SHORT).show()
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
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle(getString(R.string.dialog_load_config_select_title))
            .setItems(savedFiles) { _, which -> loadConfiguration(savedFiles[which]) }
            .show()
    }

    private fun loadConfiguration(filename: String) {
        val loadedConfig = ConfigurationManager.loadConfiguration(requireContext(), filename)
        if (loadedConfig != null) {
            segmentViewModel.segments.value = loadedConfig.segments.toMutableList()
            EffectsRepository.updateEffects(loadedConfig.effects.map { Effect(it, emptyList()) })
            if (connectionViewModel.isConnected.value == true) {
                pushConfiguration()
            }
            Toast.makeText(context, "'$filename' loaded.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to load '$filename'.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}