// app/src/main/java/com/example/android_rave_controller/SegmentConfigurationActivity.kt
package com.example.android_rave_controller

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SegmentConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segment_configuration)

        val startLedEditText = findViewById<EditText>(R.id.edit_text_start_led)
        val endLedEditText = findViewById<EditText>(R.id.edit_text_end_led)
        val saveButton = findViewById<Button>(R.id.button_save_segment)

        saveButton.setOnClickListener {
            val start = startLedEditText.text.toString()
            val end = endLedEditText.text.toString()

            if (start.isNotEmpty() && end.isNotEmpty()) {
                // Send the command to the controller
                val command = "addsegment $start $end"
                BluetoothService.sendCommand(command) // Assumes a static sendCommand method

                // Go back to the dashboard
                finish()
            } else {
                Toast.makeText(this, "Please enter both start and end LEDs", Toast.LENGTH_SHORT).show()
            }
        }
    }
}