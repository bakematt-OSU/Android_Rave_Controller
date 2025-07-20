package com.example.android_rave_controller.ui.segments

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.android_rave_controller.R
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.databinding.DialogSegmentsWizardBinding
import com.example.android_rave_controller.models.*
import com.example.android_rave_controller.support_code.setupAsColorPicker
import com.example.android_rave_controller.ui.device.DeviceViewModel
import com.google.android.material.slider.Slider

class SegmentsWizardDialogFragment : DialogFragment() {

    private var _binding: DialogSegmentsWizardBinding? = null
    private val binding get() = _binding!!

    private val segmentViewModel: SegmentViewModel by activityViewModels()
    private val effectsViewModel: EffectsViewModel by activityViewModels()
    private val deviceViewModel: DeviceViewModel by activityViewModels()

    private var maxLedCount = 0
    private var currentSelectedEffect: Effect? = null
    private val stagedParameters = mutableMapOf<String, Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSegmentsWizardBinding.inflate(inflater, container, false)
        dialog?.setTitle("Segments Wizard")
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEffectsSpinner()
        setupListeners()
        deviceViewModel.deviceProtocolHandler.liveLedCount.observe(viewLifecycleOwner) { count ->
            maxLedCount = count
        }
        CommandGetters.requestLedCount()
    }

    private fun setupEffectsSpinner() {
        val effectsAdapter = ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item_layout,
            mutableListOf()
        )
        effectsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        binding.spinnerEffect.adapter = effectsAdapter
        effectsViewModel.effects.observe(viewLifecycleOwner) { effects ->
            val effectNames = effects.map { it.name }
            effectsAdapter.clear()
            effectsAdapter.addAll(effectNames)
            effectsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupListeners() {
        binding.spinnerEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedEffectName = parent?.getItemAtPosition(position).toString()
                currentSelectedEffect = effectsViewModel.effects.value?.find { it.name == selectedEffectName }
                stagedParameters.clear()
                currentSelectedEffect?.let {
                    stagedParameters.putAll(it.parameters.associate { param -> param.name to param.value })
                    buildDynamicParametersUi(it.parameters)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.buttonCreate.setOnClickListener {
            showCreationModeDialog()
        }
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showCreationModeDialog() {
        val options = arrayOf("Start Fresh (Erase Current)", "Add to Current Configuration")
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Choose Creation Mode")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Start Fresh
                        segmentViewModel.clearAllSegments()
                        createAndAddSegments(0)
                    }
                    1 -> { // Add to Current
                        val lastSegment = segmentViewModel.segments.value?.maxByOrNull { it.endLed }
                        val nextAvailableLed = lastSegment?.endLed?.plus(1) ?: 0
                        createAndAddSegments(nextAvailableLed)
                    }
                }
            }
            .show()
    }

    private fun createAndAddSegments(startingLed: Int) {
        val numberOfSegments = binding.editTextNumberOfSegments.text.toString().toIntOrNull()
        val segmentSize = binding.editTextSegmentSize.text.toString().toIntOrNull()
        val effectName = binding.spinnerEffect.selectedItem?.toString()
        val brightness = binding.seekbarBrightness.progress

        if (numberOfSegments == null || segmentSize == null || effectName == null) {
            Toast.makeText(requireContext(), "Please fill all fields and select an effect", Toast.LENGTH_SHORT).show()
            return
        }

        val totalLedsRequired = numberOfSegments * segmentSize
        if (startingLed + totalLedsRequired > maxLedCount) {
            Toast.makeText(
                requireContext(),
                "This would require ${startingLed + totalLedsRequired} LEDs, but device only has $maxLedCount.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        var currentLed = startingLed
        for (i in 1..numberOfSegments) {
            val segment = Segment(
                id = (System.currentTimeMillis() and 0xffffffff).toInt() + i,
                name = "Segment $i",
                startLed = currentLed,
                endLed = currentLed + segmentSize - 1,
                effect = effectName,
                brightness = brightness,
                parameters = stagedParameters.toMap()
            )
            segmentViewModel.addSegment(segment)
            currentLed += segmentSize
        }
        dismiss()
    }

    private fun buildDynamicParametersUi(parameters: List<EffectParameter>) {
        binding.dynamicParametersLayout.removeAllViews()
        parameters.forEach { param ->
            val paramLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16
                    gravity = Gravity.CENTER_VERTICAL
                }
            }
            val paramNameTextView = TextView(requireContext()).apply {
                text = param.name.replace("_", " ").replaceFirstChar { it.uppercase() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            paramLayout.addView(paramNameTextView)

            when (param.type) {
                "integer", "float" -> {
                    val from = param.minVal ?: 0f
                    val to = param.maxVal ?: 255f
                    val currentValue = (stagedParameters[param.name] as? Number)?.toFloat() ?: (param.value as? Double)?.toFloat() ?: 0f
                    val step = if (param.type == "integer") 1f else 0.01f

                    val valueTextView = TextView(requireContext()).apply {
                        text = if (step == 1f) currentValue.toInt().toString() else String.format("%.2f", currentValue)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
                        textSize = 14f
                        gravity = Gravity.END
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    }

                    val slider = Slider(requireContext()).apply {
                        valueFrom = from
                        valueTo = to
                        value = currentValue.coerceIn(from, to)
                        stepSize = step
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
                        addOnChangeListener { _, value, fromUser ->
                            if (fromUser) {
                                stagedParameters[param.name] = if (stepSize == 1f) value.toInt() else value
                            }
                            valueTextView.text = if (stepSize == 1f) value.toInt().toString() else String.format("%.2f", value)
                        }
                    }
                    paramLayout.addView(slider)
                    paramLayout.addView(valueTextView)
                }
                "boolean" -> {
                    val checkBox = CheckBox(requireContext()).apply {
                        isChecked = (stagedParameters[param.name] as? Boolean) ?: (param.value as? Boolean) ?: false
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setOnCheckedChangeListener { _, isChecked ->
                            stagedParameters[param.name] = isChecked
                        }
                    }
                    paramLayout.addView(checkBox)
                }
                "color" -> {
                    val colorButton = Button(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setupAsColorPicker(
                            context = requireContext(),
                            parameterName = param.name,
                            stagedParameters = stagedParameters
                        ) { finalColor ->
                            stagedParameters[param.name] = finalColor
                        }
                    }
                    paramLayout.addView(colorButton)
                }
            }
            binding.dynamicParametersLayout.addView(paramLayout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}