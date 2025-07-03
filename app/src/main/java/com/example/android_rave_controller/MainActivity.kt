package com.example.android_rave_controller

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.android_rave_controller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Using View Binding to safely access views.
    private lateinit var binding: ActivityMainBinding

    // Companion object for constants.
    companion object {
        private const val COLOR_PICKER_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force night mode (can be removed if not needed).
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Inflate the layout and set the content view using View Binding.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the click listener for the button.
        binding.pickColorButton.setOnClickListener {
            val intent = Intent(this, ColorPickerActivity::class.java)
            // Use the new Activity Result API for better type safety and lifecycle handling.
            startActivityForResult(intent, COLOR_PICKER_REQUEST)
        }
    }

    // Handle the result from ColorPickerActivity.
    @Deprecated("This method has been deprecated in favor of the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == COLOR_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedColor = data.getIntExtra("selectedColor", Color.BLACK)

            // Get RGB values from the selected color.
            val red = Color.red(selectedColor)
            val green = Color.green(selectedColor)
            val blue = Color.blue(selectedColor)

            // Update the TextView with the new color information.
            val rgbText = "RGB: ($red, $green, $blue)"
            binding.colorTextView.text = rgbText
            binding.colorTextView.setTextColor(selectedColor)
        }
    }
}