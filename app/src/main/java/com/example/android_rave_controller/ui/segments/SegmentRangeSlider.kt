package com.example.android_rave_controller.ui.segments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.android_rave_controller.R
import com.example.android_rave_controller.models.Segment
import com.google.android.material.slider.RangeSlider

class SegmentRangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RangeSlider(context, attrs, defStyleAttr) {

    private val existingSegmentPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.purple_200) // Color for existing segments
        style = Paint.Style.FILL
    }

    private var existingSegments = listOf<Segment>()

    fun setExistingSegments(segments: List<Segment>) {
        this.existingSegments = segments
        invalidate() // Redraw the slider
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawExistingSegments(canvas)
    }

    private fun drawExistingSegments(canvas: Canvas) {
        if (existingSegments.isEmpty()) return

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