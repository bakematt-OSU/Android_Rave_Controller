package com.example.android_rave_controller.support_code

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Button
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import kotlin.math.pow

/**
 * Utility function to determine if a color is light or dark.
 * This is useful for deciding whether to place light or dark text on top of the color.
 */
fun isLightColor(color: Int): Boolean {
    val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    return darkness < 0.5
}

/**
 * Applies gamma correction to a color.
 * This is often used to make LED colors appear more visually linear and vibrant.
 * @param color The input color integer.
 * @param gamma The gamma value to apply (default is 2.8).
 * @return The gamma-corrected color integer (24-bit RGB).
 */
fun gammaCorrect(color: Int, gamma: Double = 2.8): Int {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)

    val rCorrected = (255 * (r / 255.0).pow(gamma)).toInt()
    val gCorrected = (255 * (g / 255.0).pow(gamma)).toInt()
    val bCorrected = (255 * (b / 255.0).pow(gamma)).toInt()

    // Combine back into an RGB integer and mask to ensure it's a positive 24-bit value
    return Color.rgb(rCorrected, gCorrected, bCorrected) and 0x00FFFFFF
}

/**
 * Creates and shows a standardized color picker dialog.
 * @param context The context needed to build the dialog.
 * @param title The title to display on the dialog.
 * @param initialColor The color to pre-select in the picker.
 * @param onColorSelected A lambda function to be executed when a color is chosen.
 * It receives the final, gamma-corrected color value.
 */
fun showColorPickerDialog(
    context: Context,
    title: String,
    initialColor: Int,
    onColorSelected: (Int) -> Unit
) {
    ColorPickerDialog.Builder(context)
        .setTitle(title)
        .setColorShape(ColorShape.SQAURE)
        .setDefaultColor(initialColor)
        .setColorListener(object : ColorListener {
            override fun onColorSelected(selectedColor: Int, colorHex: String) {
                val finalColorValue = selectedColor and 0x00FFFFFF // Correction is here
                Log.d("ColorPicker", "Original: $colorHex. Final Positive Int Sent: $finalColorValue")
                onColorSelected(finalColorValue)
            }
        })
        .show()
}

/**
 * Helper extension function to configure a color selection button.
 * @param context The activity/fragment context.
 * @param parameterName The name of the color parameter, used in the dialog title.
 * @param stagedParameters The map holding the currently staged parameter values.
 * @param onColorStaged A lambda to execute when a color is selected from the picker,
 * used to update the stagedParameters map.
 */
//OLD:
//fun Button.setupAsColorPicker(
//    context: Context,
//    parameterName: String,
//    stagedParameters: Map<String, Any>,
//    onColorStaged: (Int) -> Unit
//) {
//    val rawColor = stagedParameters[parameterName]
//    val colorInt = when (rawColor) {
//        is Double -> rawColor.toInt()
//        is Int -> rawColor
//        else -> Color.GRAY
//    }
//    // Ensure full alpha for display
//    val displayColor = colorInt or -0x1000000 // or 0xFF000000.toInt()
//
//    this.text = "Select Color"
//    this.setBackgroundColor(displayColor)
//    this.setTextColor(if (isLightColor(displayColor)) Color.BLACK else Color.WHITE)
//
//    this.setOnClickListener {
//        showColorPickerDialog(context, "Choose $parameterName Color", displayColor) { finalColor ->
//            // Update the UI of the button itself
//            this.setBackgroundColor(finalColor or -0x1000000)
//            this.setTextColor(if (isLightColor(finalColor)) Color.BLACK else Color.WHITE)
//            // Execute the provided lambda to update the state
//            onColorStaged(finalColor)
//        }
//    }
//}
fun Button.setupAsColorPicker(
    context: Context,
    parameterName: String,
    stagedParameters: Map<String, Any>,
    onColorStaged: (Int) -> Unit
) {
    val rawColor = stagedParameters[parameterName]

    // --- FIX IS HERE ---
    // The when block now correctly handles any Number type, preventing the default to gray.
    val colorInt = when (rawColor) {
        is Int -> rawColor
        is Double -> rawColor.toInt()
        is Number -> rawColor.toInt() // This new line handles other numeric types
        else -> Color.GRAY
    }
    // --- END OF FIX ---

    // Ensure full alpha for display
    val displayColor = colorInt or -0x1000000 // or 0xFF000000.toInt()

    this.text = "Select Color"
    this.setBackgroundColor(displayColor)
    this.setTextColor(if (isLightColor(displayColor)) Color.BLACK else Color.WHITE)

    this.setOnClickListener {
        showColorPickerDialog(context, "Choose $parameterName Color", displayColor) { finalColor ->
            // Update the UI of the button itself
            this.setBackgroundColor(finalColor or -0x1000000)
            this.setTextColor(if (isLightColor(finalColor)) Color.BLACK else Color.WHITE)
            // Execute the provided lambda to update the state
            onColorStaged(finalColor)
        }
    }
}