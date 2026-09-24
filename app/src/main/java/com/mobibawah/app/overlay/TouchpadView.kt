package com.mobibawah.app.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.mobibawah.app.R
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Area transparan ala trackpad mouse: geser jari di sini menggerakkan SATU
 * sentuhan yang ditahan & digeser terus (drag kontinu) di layar game —
 * simulasi mouse-look / geser peta.
 *
 * Ada ikon panah kecil di tengah (persis seperti di gambar referensi):
 * TAP CEPAT (tanpa menggeser) di ikon itu akan MENYALAKAN/MEMATIKAN mode
 * mouse-geser ini. Saat dimatikan, panahnya jadi pudar dan touchpad
 * berhenti "menangkap" sentuhan sama sekali — sentuhan di area itu tembus
 * langsung ke game (diatur lewat onToggleEnabled di OverlayService), jadi
 * tidak akan pernah mengganggu kontrol asli game saat kamu tidak butuh
 * fitur mouse-nya.
 */
class TouchpadView(context: Context) : FrameLayout(context) {

    companion object {
        private const val TOUCH_ID = "touchpad"
        private const val TAP_THRESHOLD_PX = 14f
    }

    var sensitivity: Float = 1.2f
    /** Dipanggil setiap kali status nyala/mati berubah, dibaca OverlayService untuk atur pass-through. */
    var onToggleEnabled: ((Boolean) -> Unit)? = null

    private var enabled = true
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var virtualX = 0f
    private var virtualY = 0f
    private var totalMove = 0f

    private val arrowIcon: TextView

    init {
        setBackgroundResource(R.drawable.shape_touchpad)
        isClickable = true

        arrowIcon = TextView(context).apply {
            text = "➤"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        addView(arrowIcon, LayoutParams(56, 56, Gravity.CENTER))
        refreshIcon()
    }

    private fun refreshIcon() {
        arrowIcon.alpha = if (enabled) 1f else 0.3f
        arrowIcon.setTextColor(if (enabled) Color.WHITE else Color.RED)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                totalMove = 0f
                if (enabled) {
                    val screenW = resources.displayMetrics.widthPixels.toFloat()
                    val screenH = resources.displayMetrics.heightPixels.toFloat()
                    virtualX = screenW / 2f
                    virtualY = screenH / 2f
                    MobiAccessibilityService.instance?.startTouch(TOUCH_ID, virtualX, virtualY)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                totalMove += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                lastRawX = event.rawX
                lastRawY = event.rawY
                if (enabled) {
                    val screenW = resources.displayMetrics.widthPixels.toFloat()
                    val screenH = resources.displayMetrics.heightPixels.toFloat()
                    virtualX = (virtualX + dx * sensitivity).coerceIn(0f, screenW)
                    virtualY = (virtualY + dy * sensitivity).coerceIn(0f, screenH)
                    MobiAccessibilityService.instance?.moveTouch(TOUCH_ID, virtualX, virtualY)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (enabled) MobiAccessibilityService.instance?.endTouch(TOUCH_ID)
                // Tap cepat tanpa geser -> nyala/matikan mode mouse
                if (totalMove < TAP_THRESHOLD_PX) {
                    enabled = !enabled
                    refreshIcon()
                    onToggleEnabled?.invoke(enabled)
                }
            }
        }
        return true
    }
}
