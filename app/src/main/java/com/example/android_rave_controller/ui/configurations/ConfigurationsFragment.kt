package com.example.android_rave_controller.ui.configurations

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.FragmentConfigurationsBinding
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsRepository
import com.example.android_rave_controller.models.SegmentViewModel
import com.example.android_rave_controller.support_code.ConfigurationManager

class ConfigurationsFragment : Fragment(), ConfigurationsAdapter.OnConfigInteractionListener {

    private var _binding: FragmentConfigurationsBinding? = null
    private val binding get() = _binding!!

    private val segmentViewModel: SegmentViewModel by activityViewModels()
    private lateinit var adapter: ConfigurationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigurationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadSavedConfigurations()
    }

    private fun setupRecyclerView() {
        adapter = ConfigurationsAdapter(mutableListOf(), this)
        binding.configurationsRecyclerView.adapter = adapter
    }

    private fun loadSavedConfigurations() {
        val savedFiles = ConfigurationManager.getSavedConfigurations(requireContext())
        adapter.updateData(savedFiles.toList())
    }

    override fun onLoad(filename: String) {
        val loadedConfig = ConfigurationManager.loadConfiguration(requireContext(), filename)
        if (loadedConfig != null) {
            segmentViewModel.segments.value = loadedConfig.segments.toMutableList()
            EffectsRepository.updateEffects(loadedConfig.effects.map { Effect(it, emptyList()) })
            Toast.makeText(context, "'$filename' loaded.", Toast.LENGTH_SHORT).show()

            // *** NAVIGATION FIX IS HERE ***
            // These options tell the navigator to pop the current screen (Configs)
            // off the back stack before navigating to the next one (Segments).
            val navOptions = navOptions {
                popUpTo(R.id.navigation_configurations) {
                    inclusive = true
                }
            }
            findNavController().navigate(R.id.navigation_segments, null, navOptions)

        } else {
            Toast.makeText(context, "Failed to load '$filename'.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRename(oldFilename: String) {
        val input = EditText(requireContext()).apply {
            setText(oldFilename)
            hint = "New configuration name"
        }
        // Apply the new theme here
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Rename Configuration")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newFilename = input.text.toString().trim()
                if (newFilename.isNotEmpty() && newFilename != oldFilename) {
                    val success = ConfigurationManager.renameConfiguration(requireContext(), oldFilename, newFilename)
                    if (success) {
                        Toast.makeText(context, "Renamed to '$newFilename'.", Toast.LENGTH_SHORT).show()
                        loadSavedConfigurations() // Refresh the list
                    } else {
                        Toast.makeText(context, "Failed to rename.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDelete(filename: String) {
        // Apply the new theme here
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Delete Configuration")
            .setMessage("Are you sure you want to delete '$filename'?")
            .setPositiveButton("Delete") { _, _ ->
                val success = ConfigurationManager.deleteConfiguration(requireContext(), filename)
                if (success) {
                    Toast.makeText(context, "'$filename' deleted.", Toast.LENGTH_SHORT).show()
                    loadSavedConfigurations() // Refresh the list
                } else {
                    Toast.makeText(context, "Failed to delete '$filename'.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}