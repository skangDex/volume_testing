package com.skangdex.volumetesting

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class VerticalSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.seekBarStyle
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Swap width and height for the underlying SeekBar logic
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Swap dimensions for vertical orientation
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        // Rotate the canvas to draw vertically
        canvas.rotate(-90f)
        canvas.translate(-height.toFloat(), 0f)
        super.onDraw(canvas)
    }

    private var mOnSeekBarChangeListener: OnSeekBarChangeListener? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        mOnSeekBarChangeListener = l
        super.setOnSeekBarChangeListener(l)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                mOnSeekBarChangeListener?.onStartTrackingTouch(this)
                attemptClaimDrag()
                updateProgress(event)
            }
            MotionEvent.ACTION_MOVE -> {
                updateProgress(event)
            }
            MotionEvent.ACTION_UP -> {
                updateProgress(event)
                mOnSeekBarChangeListener?.onStopTrackingTouch(this)
                isPressed = false
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                mOnSeekBarChangeListener?.onStopTrackingTouch(this)
                isPressed = false
            }
        }
        return true
    }

    private fun updateProgress(event: MotionEvent) {
        val newProgress = (max - (max * event.y / height).toInt()).coerceAtLeast(0).coerceAtMost(max)
        
        // Always update progress to ensure UI reflects touch
        progress = newProgress
        
        // Manually trigger onProgressChanged with fromUser = true
        // This is necessary because progress = newProgress calls setProgress(int)
        // which triggers the listener with fromUser = false.
        mOnSeekBarChangeListener?.onProgressChanged(this, newProgress, true)
        
        onSizeChanged(width, height, 0, 0)
        invalidate()
    }

    private fun attemptClaimDrag() {
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
