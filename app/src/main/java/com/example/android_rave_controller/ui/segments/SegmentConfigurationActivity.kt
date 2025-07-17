// src/main/java/com/example/android_rave_controller/ui/segments/SegmentConfigurationActivity.kt
package com.example.android_rave_controller.ui.segments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.android_rave_controller.R
import com.example.android_rave_controller.arduino_comm_ble.DeviceProtocolHandler
import com.example.android_rave_controller.databinding.ActivitySegmentConfigurationBinding
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectParameter
import com.example.android_rave_controller.models.EffectsViewModel
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class SegmentConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySegmentConfigurationBinding
    private val segmentViewModel: SegmentViewModel by viewModels()
    private val effectsViewModel: EffectsViewModel by viewModels()
    private var isUpdatingFromSlider = false
    private var segmentToEdit: Segment? = null

    private lateinit var dynamicParametersLayout: LinearLayout
    private var currentSelectedEffect: Effect? = null
    private var hasInitializedWithLedCount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySegmentConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dynamicParametersLayout = binding.dynamicParametersLayout
        segmentToEdit = intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT")

        setupStaticUI()
        setupListeners()
        setupViewModels()

        DeviceProtocolHandler.requestLedCount()
    }

    private fun setupStaticUI() {
        if (segmentToEdit != null) {
            binding.editTextSegmentName.setText(segmentToEdit!!.name)
            binding.seekbarBrightness.progress = segmentToEdit!!.brightness
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
                    binding.spinnerEffect.setSelection(effectPosition)
                }
            }
        }

        segmentViewModel.segments.observe(this) { segments ->
            binding.rangeSliderLed.setExistingSegments(segments.filter { it.id != segmentToEdit?.id })
        }

        DeviceProtocolHandler.liveLedCount.observe(this) { ledCount ->
            if (ledCount > 0 && !hasInitializedWithLedCount) {
                hasInitializedWithLedCount = true
                val slider = binding.rangeSliderLed
                slider.valueFrom = 0f
                slider.valueTo = (ledCount - 1).toFloat()

                if (segmentToEdit != null) {
                    val start = max(slider.valueFrom, segmentToEdit!!.startLed.toFloat())
                    val end = min(slider.valueTo, segmentToEdit!!.endLed.toFloat())
                    slider.values = listOf(start, end)
                } else {
                    slider.values = listOf(slider.valueFrom, min(slider.valueTo, 50f))
                }

                binding.editTextStartLed.setText(slider.values[0].toInt().toString())
                binding.editTextEndLed.setText(slider.values[1].toInt().toString())
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
            val effect = binding.spinnerEffect.selectedItem.toString()
            val brightness = binding.seekbarBrightness.progress

            if (segmentToEdit == null) {
                val newSegment = Segment(UUID.randomUUID().toString(), name, start, end, effect, brightness)
                segmentViewModel.addSegment(newSegment)
            } else {
                // **THE FIX IS HERE**
                // Using named arguments to prevent positional errors.
                val updatedSegment = segmentToEdit!!.copy(
                    name = name,
                    startLed = start,
                    endLed = end,
                    effect = effect,
                    brightness = brightness
                )
                segmentViewModel.updateSegment(updatedSegment)
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
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 16.dpToPx() }
            }
            val paramNameTextView = TextView(this).apply {
                text = param.name.replace("_", " ").replaceFirstChar { it.uppercase() }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f)
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            paramLayout.addView(paramNameTextView)

            val uiComponent: View? = when (param.type) {
                "integer", "float" -> {
                    Slider(this).apply {
                        valueFrom = param.minVal ?: 0f
                        valueTo = param.maxVal ?: 255f
                        value = (param.value as? Double)?.toFloat() ?: 0f
                        stepSize = if (param.type == "integer") 1f else 0.01f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        addOnChangeListener { _, value, fromUser ->
                            if (fromUser) {
                                val actualValue = if (stepSize == 1f) value.toInt() else value
                                segmentToEdit?.let { DeviceProtocolHandler.sendParameterUpdate(it.id, param.name, param.type, actualValue) }
                            }
                        }
                    }
                }
                "boolean" -> {
                    CheckBox(this).apply {
                        isChecked = (param.value as? Boolean) ?: false
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setOnCheckedChangeListener { _, isChecked ->
                            segmentToEdit?.let { DeviceProtocolHandler.sendParameterUpdate(it.id, param.name, param.type, isChecked) }
                        }
                    }
                }
                "color" -> {
                    Button(this).apply {
                        text = "Select Color"
                        val color = (param.value as? Double)?.toInt() ?: Color.GRAY
                        setBackgroundColor(color)
                        setTextColor(if (color.isLightColor()) Color.BLACK else Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f)
                        setOnClickListener {
                            ColorPickerDialog.Builder(this@SegmentConfigurationActivity)
                                .setTitle("Choose ${param.name} Color")
                                .setColorShape(ColorShape.SQAURE)
                                .setDefaultColor(color)
                                .setColorListener(object : ColorListener {
                                    override fun onColorSelected(selectedColor: Int, colorHex: String) {
                                        setBackgroundColor(selectedColor)
                                        setTextColor(if (selectedColor.isLightColor()) Color.BLACK else Color.WHITE)
                                        segmentToEdit?.let { DeviceProtocolHandler.sendParameterUpdate(it.id, param.name, "color", selectedColor) }
                                    }
                                })
                                .show()
                        }
                    }
                }
                else -> null
            }
            uiComponent?.let { paramLayout.addView(it) }
            dynamicParametersLayout.addView(paramLayout)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density + 0.5f).toInt()
    private fun Int.isLightColor(): Boolean = (1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255) < 0.5
}