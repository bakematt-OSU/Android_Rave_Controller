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

    // Flag to ensure LED count-dependent logic runs only once.
    private var hasInitializedWithLedCount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySegmentConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        dynamicParametersLayout = binding.dynamicParametersLayout
        segmentToEdit = intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT")

        // Setup the UI that doesn't depend on LED count yet
        setupStaticUI()
        setupListeners()
        setupViewModels()

        // Request LED count from device. The observer will handle the rest of the setup.
        DeviceProtocolHandler.requestLedCount()
    }

    /**
     * Sets up UI elements that are not dependent on the async LED count.
     */
    private fun setupStaticUI() {
        if (segmentToEdit != null) {
            binding.editTextSegmentName.setText(segmentToEdit!!.name)
            binding.seekbarBrightness.progress = segmentToEdit!!.brightness
            binding.buttonDelete.visibility = View.VISIBLE
        }
    }

    /**
     * Sets up LiveData observers. The critical logic for setting up the slider
     * is now inside the liveLedCount observer to prevent race conditions.
     */
    private fun setupViewModels() {
        val effectsAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout, mutableListOf())
        effectsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        binding.spinnerEffect.adapter = effectsAdapter

        effectsViewModel.effects.observe(this) { effects ->
            val effectNames = effects.map { it.name }
            effectsAdapter.clear()
            effectsAdapter.addAll(effectNames)
            effectsAdapter.notifyDataSetChanged()

            // Set spinner selection if editing an existing segment
            segmentToEdit?.let { segment ->
                val effectPosition = effectNames.indexOf(segment.effect)
                if (effectPosition >= 0) {
                    binding.spinnerEffect.setSelection(effectPosition)
                }
            }
        }

        segmentViewModel.segments.observe(this) { segments ->
            // Pass existing segments to the custom slider for drawing
            binding.rangeSliderLed.setExistingSegments(segments.filter { it.id != segmentToEdit?.id })
        }

        // The core of the crash fix.
        // All logic dependent on the LED count is now safely inside this observer.
        DeviceProtocolHandler.liveLedCount.observe(this) { ledCount ->
            // Ensure this initialization logic runs only once when a valid ledCount is received.
            if (ledCount > 0 && !hasInitializedWithLedCount) {
                hasInitializedWithLedCount = true
                val slider = binding.rangeSliderLed

                // 1. Set the slider's valid range FIRST.
                slider.valueFrom = 0f
                slider.valueTo = (ledCount - 1).toFloat() // Max value is count - 1

                // 2. Now, it's safe to set the slider's thumb values.
                if (segmentToEdit != null) {
                    // We are editing: Use the segment's values, but clamp them to the valid range.
                    val start = max(slider.valueFrom, segmentToEdit!!.startLed.toFloat())
                    val end = min(slider.valueTo, segmentToEdit!!.endLed.toFloat())
                    slider.values = listOf(start, end)
                } else {
                    // We are creating a new segment: Set default values.
                    slider.values = listOf(slider.valueFrom, min(slider.valueTo, 50f))
                }

                // Update EditTexts to reflect the (potentially clamped) slider values
                binding.editTextStartLed.setText(slider.values[0].toInt().toString())
                binding.editTextEndLed.setText(slider.values[1].toInt().toString())
            }
        }
    }

    /**
     * Sets up all the necessary listeners for the UI components.
     */
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

                currentSelectedEffect?.let { effect ->
                    if (effect.parameters.isEmpty()) {
                        DeviceProtocolHandler.requestEffectParameters(effect.name)
                    } else {
                        buildDynamicParametersUi(effect.parameters)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { /* Do nothing */ }
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
                val newSegment = Segment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    startLed = start,
                    endLed = end,
                    effect = effect,
                    brightness = brightness
                )
                segmentViewModel.addSegment(newSegment)
                Toast.makeText(this, "$name saved", Toast.LENGTH_SHORT).show()
            } else {
                val updatedSegment = segmentToEdit!!.copy(
                    name = name,
                    startLed = start,
                    endLed = end,
                    effect = effect,
                    brightness = brightness
                )
                segmentViewModel.updateSegment(updatedSegment)
                Toast.makeText(this, "$name updated", Toast.LENGTH_SHORT).show()
            }
            // Logic to handle saving dynamic parameters would go here
            finish()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        binding.buttonDelete.setOnClickListener {
            segmentToEdit?.let {
                segmentViewModel.deleteSegment(it.id)
                Toast.makeText(this, "${it.name} deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun buildDynamicParametersUi(parameters: Map<String, Any>) {
        dynamicParametersLayout.removeAllViews()

        parameters.forEach { (paramName, paramValue) ->
            val paramLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16.dpToPx() }
            }

            val paramNameTextView = TextView(this).apply {
                text = paramName
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.3f
                )
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            paramLayout.addView(paramNameTextView)

            val uiComponent: View? = when {
                paramName.contains("color", ignoreCase = true) && paramValue is Int -> {
                    Button(this).apply {
                        text = "Select Color"
                        setBackgroundColor(paramValue)
                        setTextColor(if (paramValue.isLightColor()) Color.BLACK else Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                        setOnClickListener {
                            ColorPickerDialog.Builder(this@SegmentConfigurationActivity)
                                .setTitle("Choose $paramName Color")
                                .setColorShape(ColorShape.SQAURE)
                                .setDefaultColor(paramValue)
                                .setColorListener(object : ColorListener {
                                    override fun onColorSelected(color: Int, colorHex: String) {
                                        setBackgroundColor(color)
                                        setTextColor(if (color.isLightColor()) Color.BLACK else Color.WHITE)
                                        currentSelectedEffect = currentSelectedEffect?.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = color })
                                    }
                                })
                                .show()
                        }
                    }
                }
                paramValue is Number -> {
                    Slider(this).apply {
                        valueFrom = 0f // Placeholder
                        valueTo = 255f // Placeholder
                        value = paramValue.toFloat()
                        stepSize = if (paramValue is Int || paramValue is Long) 1f else 0.01f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                        addOnChangeListener { _, value, _ ->
                            val actualValue: Any = if (paramValue is Int || paramValue is Long) value.toInt() else value
                            currentSelectedEffect = currentSelectedEffect?.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = actualValue })
                        }
                    }
                }
                paramValue is Boolean -> {
                    CheckBox(this).apply {
                        isChecked = paramValue
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                        setOnCheckedChangeListener { _, isChecked ->
                            currentSelectedEffect = currentSelectedEffect?.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = isChecked })
                        }
                    }
                }
                else -> null
            }

            uiComponent?.let {
                it.tag = paramName
                paramLayout.addView(it)
            }
            dynamicParametersLayout.addView(paramLayout)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun Int.isLightColor(): Boolean {
        val darkness = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
        return darkness < 0.5
    }
}