package com.example.android_rave_controller

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels // The SINGLE, CORRECT import
import androidx.appcompat.app.AppCompatActivity
import com.example.android_rave_controller.models.EffectsViewModel
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import java.util.UUID

class SegmentConfigurationActivity : AppCompatActivity() {

    private val segmentViewModel: SegmentViewModel by viewModels()
    private val effectsViewModel: EffectsViewModel by viewModels()
    private var isUpdatingFromSlider = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_configuration)

        val nameEditText = findViewById<EditText>(R.id.edit_text_segment_name)
        val startLedEditText = findViewById<EditText>(R.id.edit_text_start_led)
        val endLedEditText = findViewById<EditText>(R.id.edit_text_end_led)
        val ledRangeSlider = findViewById<SegmentRangeSlider>(R.id.range_slider_led)
        val effectSpinner = findViewById<Spinner>(R.id.spinner_effect)
        val brightnessSeekBar = findViewById<SeekBar>(R.id.seekbar_brightness)
        val saveButton = findViewById<Button>(R.id.button_save_segment)
        val cancelButton = findViewById<Button>(R.id.button_cancel)
        val deleteButton = findViewById<Button>(R.id.button_delete)

        val effectsAdapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout, mutableListOf())
        effectsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        effectSpinner.adapter = effectsAdapter

        effectsViewModel.effects.observe(this) { effects ->
            effectsAdapter.clear()
            effectsAdapter.addAll(effects)
            effectsAdapter.notifyDataSetChanged()
        }

        segmentViewModel.segments.observe(this) { segments ->
            ledRangeSlider.setExistingSegments(segments)
        }

        ledRangeSlider.addOnChangeListener { slider, _, _ ->
            isUpdatingFromSlider = true
            val start = slider.values[0].toInt()
            val end = slider.values[1].toInt()
            startLedEditText.setText(start.toString())
            endLedEditText.setText(end.toString())
            isUpdatingFromSlider = false
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingFromSlider) {
                    val start = startLedEditText.text.toString().toFloatOrNull() ?: ledRangeSlider.values[0]
                    val end = endLedEditText.text.toString().toFloatOrNull() ?: ledRangeSlider.values[1]
                    if (start <= end && start >= ledRangeSlider.valueFrom && end <= ledRangeSlider.valueTo) {
                        ledRangeSlider.values = listOf(start, end)
                    }
                }
            }
        }
        startLedEditText.addTextChangedListener(textWatcher)
        endLedEditText.addTextChangedListener(textWatcher)

        startLedEditText.setText(ledRangeSlider.values[0].toInt().toString())
        endLedEditText.setText(ledRangeSlider.values[1].toInt().toString())

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val start = ledRangeSlider.values[0].toInt()
            val end = ledRangeSlider.values[1].toInt()
            val effect = effectSpinner.selectedItem.toString()
            val brightness = brightnessSeekBar.progress

            if (name.isNotEmpty()) {
                val newSegment = Segment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    startLed = start,
                    endLed = end,
                    effect = effect,
                    brightness = brightness
                )
                segmentViewModel.addSegment(newSegment)

                val newSegmentIndex = (segmentViewModel.segments.value?.size ?: 1) - 1

                DeviceProtocolHandler.setSegmentRange(newSegmentIndex, start, end)
                DeviceProtocolHandler.selectSegment(newSegmentIndex)
                val effectIndex = effectsViewModel.effects.value?.indexOf(effect) ?: 0
                DeviceProtocolHandler.setEffect(effectIndex)
                DeviceProtocolHandler.setSegmentBrightness(newSegmentIndex, brightness)

                Toast.makeText(this, "$name saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter a segment name", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }

        deleteButton.setOnClickListener {
            Toast.makeText(this, "Delete clicked", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}