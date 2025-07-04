package com.example.android_rave_controller

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.android_rave_controller.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val colorPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.let {
                val selectedColor = it.getIntExtra("selectedColor", Color.BLACK)
                val red = Color.red(selectedColor)
                val green = Color.green(selectedColor)
                val blue = Color.blue(selectedColor)
                val rgbText = "RGB: ($red, $green, $blue)"
                binding.colorTextView.text = rgbText
                binding.colorTextView.setTextColor(selectedColor)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickColorButton.setOnClickListener {
            val intent = Intent(this, ColorPickerActivity::class.java)
            colorPickerLauncher.launch(intent)
        }
    }
}