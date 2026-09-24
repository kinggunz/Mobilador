package com.mobibawah.app.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Representasi touchpad (area mouse-geser) DI DALAM layar edit mapping.
 * Muncul OTOMATIS setiap kali membuka editor (tidak perlu ditambah manual
 * seperti tombol lain) karena touchpad memang bagian bawaan setiap game —
 * cukup digeser ke posisi yang pas, dan tap untuk atur ukuran/sensitivitas.
 */
class TouchpadEditView(
    context: Context,
    private val onMoved: ((dxFrac: Float, dyFrac: Float) -> Unit)? = null,
    private val onTapped: (() -> Unit)? = null
) : FrameLayout(context) {

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var totalMove = 0f

    init {
        val bg = GradientDrawable()
        bg.setColor(0x332196F3)
        bg.setStroke(3, Color.parseColor("#2196F3"))
        bg.cornerRadius = 20f
        background = bg
        isClickable = true

        val label = TextView(context).apply {
            text = "➤ MOUSE / GESER LAYAR"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val parentView = parent as? android.view.View
        val parentW = parentView?.width?.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val parentH = parentView?.height?.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX; lastRawY = event.rawY
                totalMove = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                totalMove += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                lastRawX = event.rawX; lastRawY = event.rawY
                onMoved?.invoke(dx / parentW, dy / parentH)
            }
            MotionEvent.ACTION_UP -> {
                if (totalMove < 12f) onTapped?.invoke()
            }
        }
        return true
    }
}
