package com.mobibawah.app.overlay

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import com.mobibawah.app.R
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Area transparan ala trackpad mouse: geser jari di sini akan menggerakkan
 * SATU sentuhan yang benar-benar "ditahan dan digeser" (drag kontinu) di
 * tengah layar game — persis seperti kamu menggeser peta/kamera langsung
 * pakai jari, hanya saja arah & jaraknya mengikuti gerakan jari di
 * touchpad (dikalikan sensitivity), bukan posisi jari yang sebenarnya.
 *
 * Ini dipakai untuk 2 kebutuhan sekaligus:
 *  - Simulasi gerakan mouse/kamera (game FPS/TPS gaya PC).
 *  - Geser-geser layar biasa (scroll peta, dsb) untuk game yang kontrolnya
 *    memang berbasis drag jari.
 */
class TouchpadView(context: Context) : FrameLayout(context) {

    companion object {
        private const val TOUCH_ID = "touchpad"
    }

    var sensitivity: Float = 1.2f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var virtualX = 0f
    private var virtualY = 0f

    init {
        setBackgroundResource(R.drawable.shape_touchpad)
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val service = MobiAccessibilityService.instance ?: return true
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                // Mulai drag dari titik tengah layar game
                virtualX = screenW / 2f
                virtualY = screenH / 2f
                service.startTouch(TOUCH_ID, virtualX, virtualY)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX) * sensitivity
                val dy = (event.rawY - lastRawY) * sensitivity
                lastRawX = event.rawX
                lastRawY = event.rawY
                virtualX = (virtualX + dx).coerceIn(0f, screenW)
                virtualY = (virtualY + dy).coerceIn(0f, screenH)
                service.moveTouch(TOUCH_ID, virtualX, virtualY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                service.endTouch(TOUCH_ID)
            }
        }
        return true
    }
}
