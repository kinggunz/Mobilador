package com.mobibawah.app.overlay

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import com.mobibawah.app.R
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Area drag mouse-look — SEKARANG dengan 2 CHANNEL INDEPENDEN yang
 * dibedakan dari tombol mouse mana yang dipakai memulai drag:
 *
 *  - Drag dengan KLIK KIRI mouse (atau jari, karena jari tidak punya info
 *    tombol) -> channel "touchpad_left", pakai sensitivityLeft sendiri.
 *  - Drag dengan KLIK KANAN mouse -> channel "touchpad_right", pakai
 *    sensitivityRight sendiri.
 *
 * Keduanya disimulasikan sebagai sentuhan yang BENAR-BENAR TERPISAH (id
 * berbeda) ke MobiAccessibilityService, jadi berperilaku seperti 2 mode
 * lihat sekitar yang independen — cocok untuk mis. klik kiri = look
 * normal, klik kanan = mode lain (aim presisi dengan sensitivitas lebih
 * rendah, dsb), sesuai kebutuhan masing-masing game.
 */
class TouchpadView(context: Context) : FrameLayout(context) {

    companion object {
        const val TOUCH_ID_LEFT = "touchpad_left"
        const val TOUCH_ID_RIGHT = "touchpad_right"
    }

    var sensitivityLeft: Float = 1.2f
    var sensitivityRight: Float = 1.2f

    private var activeTouchId: String? = null
    private var activeSensitivity: Float = 1.2f
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
                // Cek tombol mouse yang menekan: klik kanan = channel terpisah.
                // Jari (bukan mouse) tidak set buttonState apa pun -> default channel kiri.
                val isRightClick = (event.buttonState and MotionEvent.BUTTON_SECONDARY) != 0
                activeTouchId = if (isRightClick) TOUCH_ID_RIGHT else TOUCH_ID_LEFT
                activeSensitivity = if (isRightClick) sensitivityRight else sensitivityLeft

                lastRawX = event.rawX
                lastRawY = event.rawY
                virtualX = screenW / 2f
                virtualY = screenH / 2f
                MobiAccessibilityService.instance?.startTouch(activeTouchId!!, virtualX, virtualY)
            }
            MotionEvent.ACTION_MOVE -> {
                val id = activeTouchId ?: return true
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                virtualX = (virtualX + dx * activeSensitivity).coerceIn(0f, screenW)
                virtualY = (virtualY + dy * activeSensitivity).coerceIn(0f, screenH)
                MobiAccessibilityService.instance?.moveTouch(id, virtualX, virtualY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeTouchId?.let { MobiAccessibilityService.instance?.endTouch(it) }
                activeTouchId = null
            }
        }
        return true
    }
}
