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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.android_rave_controller.R
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsViewModel
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import com.example.android_rave_controller.arduino_comm_ble.DeviceProtocolHandler
import com.example.android_rave_controller.databinding.ActivitySegmentConfigurationBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.slider.RangeSlider // Import RangeSlider
import com.google.android.material.slider.Slider // Keep Slider import if used elsewhere for single thumb slider

import java.util.UUID

class SegmentConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySegmentConfigurationBinding
    private val segmentViewModel: SegmentViewModel by viewModels()
    private val effectsViewModel: EffectsViewModel by viewModels()
    private var isUpdatingFromSlider = false
    private var segmentToEdit: Segment? = null

    private lateinit var dynamicParametersLayout: LinearLayout
    private var currentSelectedEffect: Effect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySegmentConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        segmentToEdit = intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT")

        // Use binding to access views - ALL REFERENCES ARE NOW CORRECTED
        val nameEditText: EditText = binding.editTextSegmentName
        val startLedEditText: EditText = binding.editTextStartLed
        val endLedEditText: EditText = binding.editTextEndLed
        val ledRangeSlider: SegmentRangeSlider = binding.rangeSliderLed
        val effectSpinner: Spinner = binding.spinnerEffect
        val brightnessSeekBar: SeekBar = binding.seekbarBrightness
        val saveButton: Button = binding.buttonSaveSegment
        val cancelButton: Button = binding.buttonCancel
        val deleteButton: Button = binding.buttonDelete

        // Initialize dynamic parameters layout
        dynamicParametersLayout = binding.dynamicParametersLayout

        val effectsAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout, mutableListOf())
        effectsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        effectSpinner.adapter = effectsAdapter

        effectsViewModel.effects.observe(this) { effects: List<Effect> ->
            val effectNames = effects.map { it.name }
            effectsAdapter.clear()
            effectsAdapter.addAll(effectNames)
            effectsAdapter.notifyDataSetChanged()

            segmentToEdit?.let { segment ->
                val effectPosition = effectNames.indexOf(segment.effect)
                if (effectPosition >= 0) {
                    effectSpinner.setSelection(effectPosition)
                    currentSelectedEffect = effects.find { it.name == segment.effect }
                    currentSelectedEffect?.let { effect ->
                        if (effect.parameters.isNotEmpty()) {
                            buildDynamicParametersUi(effect.parameters)
                        } else {
                            DeviceProtocolHandler.requestEffectParameters(effect.name)
                        }
                    }
                }
            }
            currentSelectedEffect?.let { selectedEffect ->
                val updatedEffect = effects.find { it.name == selectedEffect.name }
                if (updatedEffect != null && updatedEffect.parameters.isNotEmpty() && updatedEffect != selectedEffect) {
                    currentSelectedEffect = updatedEffect
                    buildDynamicParametersUi(updatedEffect.parameters)
                }
            }
        }

        segmentViewModel.segments.observe(this) { segments: List<Segment> ->
            ledRangeSlider.setExistingSegments(segments)
        }

        DeviceProtocolHandler.liveLedCount.observe(this) { ledCount: Int ->
            ledRangeSlider.valueTo = ledCount.toFloat()
            segmentToEdit?.let {
                if (it.endLed.toFloat() >= ledCount.toFloat()) {
                    ledRangeSlider.values = listOf(it.startLed.toFloat(), (ledCount - 1).toFloat())
                    endLedEditText.setText((ledCount - 1).toString())
                }
            }
            if (segmentToEdit == null && endLedEditText.text.toString().isEmpty()) {
                startLedEditText.setText(ledRangeSlider.getValues()[0].toInt().toString()) // Use getValues()
                endLedEditText.setText(ledRangeSlider.getValues()[1].toInt().toString()) // Use getValues()
            }
        }
        DeviceProtocolHandler.requestLedCount()


        // Corrected type for `slider` parameter to RangeSlider
        ledRangeSlider.addOnChangeListener { slider: RangeSlider, _, _ ->
            isUpdatingFromSlider = true
            val start = slider.getValues()[0].toInt() // Use getValues()
            val end = slider.getValues()[1].toInt() // Use getValues()
            startLedEditText.setText(start.toString())
            endLedEditText.setText(end.toString())
            isUpdatingFromSlider = false
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingFromSlider) {
                    val start = s.toString().toFloatOrNull() ?: ledRangeSlider.getValues()[0] // Use getValues()
                    val end = endLedEditText.text.toString().toFloatOrNull() ?: ledRangeSlider.getValues()[1] // Use getValues()
                    // Added explicit type for `start` and `end`
                    val startFloat: Float = start
                    val endFloat: Float = end

                    if (startFloat <= endFloat && startFloat >= ledRangeSlider.valueFrom && endFloat <= ledRangeSlider.valueTo) {
                        ledRangeSlider.values = listOf(startFloat, endFloat)
                    }
                }
            }
        }
        startLedEditText.addTextChangedListener(textWatcher)
        endLedEditText.addTextChangedListener(textWatcher)

        if (segmentToEdit != null) {
            nameEditText.setText(segmentToEdit!!.name)
            if (segmentToEdit!!.endLed.toFloat() > ledRangeSlider.valueTo) {
                ledRangeSlider.valueTo = segmentToEdit!!.endLed.toFloat()
            }
            ledRangeSlider.values = listOf(segmentToEdit!!.startLed.toFloat(), segmentToEdit!!.endLed.toFloat())
            brightnessSeekBar.progress = segmentToEdit!!.brightness
            deleteButton.visibility = View.VISIBLE
        } else {
            startLedEditText.setText(ledRangeSlider.getValues()[0].toInt().toString()) // Use getValues()
            endLedEditText.setText(ledRangeSlider.getValues()[1].toInt().toString()) // Use getValues()
        }

        effectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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


        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val start = ledRangeSlider.getValues()[0].toInt() // Use getValues()
            val end = ledRangeSlider.getValues()[1].toInt() // Use getValues()
            val effect = effectSpinner.selectedItem.toString()
            val brightness = brightnessSeekBar.progress

            if (name.isNotEmpty()) {
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
                currentSelectedEffect?.let { effectWithParams ->
                    for ((paramName, paramValue) in effectWithParams.parameters) {
                        val paramView = dynamicParametersLayout.findViewWithTag<View>(paramName)
                        val updatedValue: Any = when (paramView) {
                            is Slider -> paramView.value.let { if (effectWithParams.parameters[paramName] is Int) it.toInt() else it.toFloat() }
                            is CheckBox -> paramView.isChecked
                            is Button -> (paramView.background as? android.graphics.drawable.ColorDrawable)?.color ?: (effectWithParams.parameters[paramName] as? Int) ?: 0
                            else -> paramValue
                        }
                    }
                }
                finish()
            } else {
                Toast.makeText(this, "Please enter a segment name", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }

        deleteButton.setOnClickListener {
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
                paramValue is Int && (paramName.contains("color", ignoreCase = true) || (paramValue >= 0 && paramValue <= 0xFFFFFF)) -> {
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
                                        currentSelectedEffect = currentSelectedEffect!!.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = color })
                                    }
                                })
                                .show()
                        }
                    }
                }
                paramValue is Number -> {
                    val sliderValue = paramValue.toFloat()
                    Slider(this).apply {
                        valueFrom = 0f
                        valueTo = 255f
                        value = sliderValue
                        stepSize = if (paramValue is Int) 1f else 0.01f

                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                        addOnChangeListener { _, value: Float, _ ->
                            val actualValue: Any = if (paramValue is Int) value.toInt() else value
                            currentSelectedEffect = currentSelectedEffect!!.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = actualValue })
                        }
                    }
                }
                paramValue is Boolean -> {
                    CheckBox(this).apply {
                        isChecked = paramValue
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
                        setOnCheckedChangeListener { _, isChecked ->
                            currentSelectedEffect = currentSelectedEffect!!.copy(parameters = currentSelectedEffect!!.parameters.toMutableMap().apply { this[paramName] = isChecked })
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