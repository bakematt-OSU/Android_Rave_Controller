package com.example.android_rave_controller.ui.segments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.android_rave_controller.R
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.databinding.ActivitySegmentConfigurationBinding
import com.example.android_rave_controller.models.*
import com.example.android_rave_controller.support_code.setupAsColorPicker
import com.example.android_rave_controller.ui.device.DeviceViewModel
import com.google.android.material.slider.Slider
import kotlin.math.max
import kotlin.math.min

class SegmentConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySegmentConfigurationBinding
    private val segmentViewModel: SegmentViewModel by viewModels()
    private val effectsViewModel: EffectsViewModel by viewModels()
    private val deviceViewModel: DeviceViewModel by viewModels()

    private var isUpdatingFromSlider = false
    private var segmentToEdit: Segment? = null
    private lateinit var dynamicParametersLayout: LinearLayout
    private var currentSelectedEffect: Effect? = null
    private var hasInitializedWithLedCount = false
    private val stagedParameters = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySegmentConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dynamicParametersLayout = binding.dynamicParametersLayout

        segmentToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT", Segment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT")
        }

        setupStaticUI()
        setupListeners()
        setupViewModels()
        CommandGetters.requestLedCount()
    }

    private fun setupStaticUI() {
        segmentToEdit?.let {
            binding.editTextSegmentName.setText(it.name)
            binding.seekbarBrightness.progress = it.brightness
            binding.buttonDelete.visibility = View.VISIBLE
        }
    }

    private fun setupViewModels() {
        val effectsAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout, mutableListOf())
        effectsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        binding.spinnerEffect.adapter = effectsAdapter

        effectsViewModel.effects.observe(this) { effects ->
            val effectNames = effects.map { it.name }
            effectsAdapter.clear()
            effectsAdapter.addAll(effectNames)
            effectsAdapter.notifyDataSetChanged()
            segmentToEdit?.let { segment ->
                val effectPosition = effectNames.indexOf(segment.effect)
                if (effectPosition >= 0) {
                    binding.spinnerEffect.setSelection(effectPosition, false)
                }
            }
        }

        deviceViewModel.deviceProtocolHandler.liveLedCount.observe(this) { ledCount ->
            if (ledCount > 0) {
                val slider = binding.rangeSliderLed
                slider.valueFrom = 0f
                slider.valueTo = (ledCount - 1).toFloat()

                if (!hasInitializedWithLedCount) {
                    hasInitializedWithLedCount = true
                    segmentToEdit?.let {
                        slider.values = listOf(max(slider.valueFrom, it.startLed.toFloat()), min(slider.valueTo, it.endLed.toFloat()))
                    } ?: run {
                        slider.values = listOf(slider.valueFrom, min(slider.valueTo, 50f))
                    }
                    binding.editTextStartLed.setText(slider.values[0].toInt().toString())
                    binding.editTextEndLed.setText(slider.values[1].toInt().toString())
                } else {
                    slider.values = listOf(min(slider.values[0], slider.valueTo), min(slider.values[1], slider.valueTo))
                }
            }
        }
    }

    private fun setupListeners() {
        binding.rangeSliderLed.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                isUpdatingFromSlider = true
                binding.editTextStartLed.setText(slider.values[0].toInt().toString())
                binding.editTextEndLed.setText(slider.values[1].toInt().toString())
                isUpdatingFromSlider = false
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromSlider || !hasInitializedWithLedCount) return
                val slider = binding.rangeSliderLed
                val start = binding.editTextStartLed.text.toString().toFloatOrNull() ?: slider.values[0]
                val end = binding.editTextEndLed.text.toString().toFloatOrNull() ?: slider.values[1]
                if (start <= end && start >= slider.valueFrom && end <= slider.valueTo) {
                    slider.values = listOf(start, end)
                }
            }
        }
        binding.editTextStartLed.addTextChangedListener(textWatcher)
        binding.editTextEndLed.addTextChangedListener(textWatcher)

        binding.spinnerEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedEffectName = parent?.getItemAtPosition(position).toString()
                currentSelectedEffect = effectsViewModel.effects.value?.find { it.name == selectedEffectName }

                val defaultParams = currentSelectedEffect?.parameters?.associate { it.name to it.value }.orEmpty()
                val savedParams = segmentToEdit?.parameters.orEmpty()

                stagedParameters.clear()
                stagedParameters.putAll(defaultParams)
                stagedParameters.putAll(savedParams)

                dynamicParametersLayout.removeAllViews()
                currentSelectedEffect?.let { buildDynamicParametersUi(it.parameters) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.buttonSaveSegment.setOnClickListener {
            val name = binding.editTextSegmentName.text.toString()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a segment name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val start = binding.rangeSliderLed.values[0].toInt()
            val end = binding.rangeSliderLed.values[1].toInt()
            val effectName = binding.spinnerEffect.selectedItem.toString()
            val brightness = binding.seekbarBrightness.progress

            val segmentToSave = segmentToEdit?.copy(
                name = name, startLed = start, endLed = end, effect = effectName, brightness = brightness, parameters = stagedParameters
            ) ?: Segment(
                id = (System.currentTimeMillis() and 0xffffffff).toInt(),
                name = name,
                startLed = start,
                endLed = end,
                effect = effectName,
                brightness = brightness,
                parameters = stagedParameters
            )

            if (segmentToEdit == null) {
                segmentViewModel.addSegment(segmentToSave)
            } else {
                segmentViewModel.updateSegment(segmentToSave)
            }
            finish()
        }

        binding.buttonCancel.setOnClickListener { finish() }
        binding.buttonDelete.setOnClickListener {
            segmentToEdit?.let {
                segmentViewModel.deleteSegment(it.id)
                finish()
            }
        }
    }

    private fun buildDynamicParametersUi(parameters: List<EffectParameter>) {
        dynamicParametersLayout.removeAllViews()
        parameters.forEach { param ->
            val paramLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16.dpToPx()
                    gravity = Gravity.CENTER_VERTICAL
                }
            }
            val paramNameTextView = TextView(this).apply {
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

                    val valueTextView = TextView(this).apply {
                        text = if (step == 1f) currentValue.toInt().toString() else String.format("%.2f", currentValue)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
                        textSize = 14f
                        gravity = Gravity.END
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    }

                    val slider = Slider(this).apply {
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
                    val checkBox = CheckBox(this).apply {
                        isChecked = (stagedParameters[param.name] as? Boolean) ?: (param.value as? Boolean) ?: false
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setOnCheckedChangeListener { _, isChecked ->
                            stagedParameters[param.name] = isChecked
                        }
                    }
                    paramLayout.addView(checkBox)
                }
                "color" -> {
                    val colorButton = Button(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setupAsColorPicker(
                            context = this@SegmentConfigurationActivity,
                            parameterName = param.name,
                            stagedParameters = stagedParameters
                        ) { finalColor ->
                            stagedParameters[param.name] = finalColor
                        }
                    }
                    paramLayout.addView(colorButton)
                }
            }
            dynamicParametersLayout.addView(paramLayout)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
}