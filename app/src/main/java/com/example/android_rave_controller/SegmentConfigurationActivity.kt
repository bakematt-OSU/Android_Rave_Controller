package com.example.android_rave_controller

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.android_rave_controller.models.Effect
import com.example.android_rave_controller.models.EffectsViewModel
import com.example.android_rave_controller.models.Segment
import com.example.android_rave_controller.models.SegmentViewModel
import java.util.UUID

class SegmentConfigurationActivity : AppCompatActivity() {

    private val segmentViewModel: SegmentViewModel by viewModels()
    private val effectsViewModel: EffectsViewModel by viewModels()
    private var isUpdatingFromSlider = false
    private var segmentToEdit: Segment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_configuration)

        segmentToEdit = intent.getParcelableExtra("EXTRA_SEGMENT_TO_EDIT")

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

        // **FIX**: Observe the LiveData<List<Effect>> directly.
        // The transformation from Effect to String happens inside the observer.
        effectsViewModel.effects.observe(this) { effects ->
            val effectNames = effects.map { it.name }
            effectsAdapter.clear()
            effectsAdapter.addAll(effectNames)
            effectsAdapter.notifyDataSetChanged()

            segmentToEdit?.let {
                val effectPosition = effectNames.indexOf(it.effect)
                if (effectPosition >= 0) {
                    effectSpinner.setSelection(effectPosition)
                }
            }
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

        if (segmentToEdit != null) {
            nameEditText.setText(segmentToEdit!!.name)
            if (segmentToEdit!!.endLed > ledRangeSlider.valueTo) {
                ledRangeSlider.valueTo = segmentToEdit!!.endLed.toFloat()
            }
            ledRangeSlider.values = listOf(segmentToEdit!!.startLed.toFloat(), segmentToEdit!!.endLed.toFloat())
            brightnessSeekBar.progress = segmentToEdit!!.brightness
            deleteButton.visibility = View.VISIBLE
        } else {
            startLedEditText.setText(ledRangeSlider.values[0].toInt().toString())
            endLedEditText.setText(ledRangeSlider.values[1].toInt().toString())
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val start = ledRangeSlider.values[0].toInt()
            val end = ledRangeSlider.values[1].toInt()
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
}