// app/src/main/java/com/example/android_rave_controller/ui/segments/SegmentRangeSlider.kt
package com.example.android_rave_controller.ui.segments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.android_rave_controller.R
import com.example.android_rave_controller.models.Segment
import com.google.android.material.slider.RangeSlider
import kotlin.math.max
import kotlin.math.min

class SegmentRangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RangeSlider(context, attrs, defStyleAttr) {

    private val existingSegmentPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.purple_500) // Color for existing segments
        style = Paint.Style.FILL
    }

    private var existingSegments = listOf<Segment>()

    fun setExistingSegments(segments: List<Segment>) {
        this.existingSegments = segments
        invalidate() // Redraw the slider
    }

    override fun onDraw(canvas: Canvas) {
        // 🔹 **CRASH FIX** 🔹
        // Clamp the active thumb values to be within the allowed range before drawing.
        // This must be done *before* super.onDraw() is called.
        if (values.size >= 2) {
            val clampedValues = values.map {
                min(max(it, valueFrom), valueTo)
            }
            if (values != clampedValues) {
                setValues(clampedValues)
            }
        }

        // Now, call the original onDraw and your custom drawing method
        super.onDraw(canvas)
        drawExistingSegments(canvas)
    }

    private fun drawExistingSegments(canvas: Canvas) {
        if (existingSegments.isEmpty() || valueTo <= 0) return

        val trackWidth = width - paddingStart - paddingEnd
        val trackTop = (height / 2f) - (trackHeight / 2f)
        val trackBottom = (height / 2f) + (trackHeight / 2f)

        for (segment in existingSegments) {
            val startX = paddingStart + (segment.startLed / valueTo) * trackWidth
            val endX = paddingStart + (segment.endLed / valueTo) * trackWidth

            canvas.drawRect(startX, trackTop, endX, trackBottom, existingSegmentPaint)
        }
    }
}