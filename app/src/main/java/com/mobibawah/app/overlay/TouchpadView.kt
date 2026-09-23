package com.mobibawah.app.overlay

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.mobibawah.app.R
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Area transparan yang berfungsi seperti trackpad mouse: geser jari di area
 * ini akan diterjemahkan menjadi rangkaian swipe pendek di titik tengah
 * layar game, sehingga terasa seperti menggerakkan kamera/mouse.
 *
 * sensitivity mengalikan jarak swipe supaya gerakan bisa dibuat lebih halus
 * atau lebih responsif sesuai selera game.
 */
class TouchpadView(context: Context) : FrameLayout(context) {

    var sensitivity: Float = 1.2f
    private var lastX = 0f
    private var lastY = 0f
    private var anchorX = 0f
    private var anchorY = 0f

    init {
        setBackgroundResource(R.drawable.shape_touchpad)
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val service = MobiAccessibilityService.instance
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX; lastY = event.rawY
                anchorX = screenW / 2f; anchorY = screenH / 2f
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX) * sensitivity
                val dy = (event.rawY - lastY) * sensitivity
                lastX = event.rawX; lastY = event.rawY
                val fromX = anchorX
                val fromY = anchorY
                val toX = (anchorX + dx).coerceIn(0f, screenW)
                val toY = (anchorY + dy).coerceIn(0f, screenH)
                service?.performSwipe(fromX, fromY, toX, toY, 25L)
            }
        }
        return true
    }
}
