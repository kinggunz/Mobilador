package com.mobibawah.app.overlay

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import com.mobibawah.app.R
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Area drag mouse-look murni: geser jari di sini menggerakkan SATU sentuhan
 * yang ditahan & digeser terus (drag kontinu) di layar game — simulasi
 * mouse-look / geser peta ala FreeFire dkk.
 *
 * View ini TIDAK lagi mengurus nyala/mati sendiri (lihat OverlayService):
 * saat mode drag OFF, window-nya sendiri dibuat FLAG_NOT_TOUCHABLE oleh
 * OverlayService sehingga onTouchEvent di sini bahkan tidak pernah
 * dipanggil sama sekali — sentuhan tembus 100% ke game. Ikon kursor
 * (nyala/matikan) sekarang jadi window TERPISAH yang SELALU bisa
 * disentuh, supaya tidak pernah "terkunci tidak bisa disentuh lagi".
 */
class TouchpadView(context: Context) : FrameLayout(context) {

    companion object {
        const val TOUCH_ID = "touchpad"
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
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                virtualX = screenW / 2f
                virtualY = screenH / 2f
                MobiAccessibilityService.instance?.startTouch(TOUCH_ID, virtualX, virtualY)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                virtualX = (virtualX + dx * sensitivity).coerceIn(0f, screenW)
                virtualY = (virtualY + dy * sensitivity).coerceIn(0f, screenH)
                MobiAccessibilityService.instance?.moveTouch(TOUCH_ID, virtualX, virtualY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                MobiAccessibilityService.instance?.endTouch(TOUCH_ID)
            }
        }
        return true
    }
}
