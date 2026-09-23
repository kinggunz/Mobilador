package com.mobibawah.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Satu tombol keyboard virtual di layar.
 *
 * Dua mode:
 *  - MODE MAIN   : sentuhan pada tombol -> dikirim ke MobiAccessibilityService
 *                   sebagai tap/hold/toggle di targetX/targetY (koordinat game).
 *  - MODE EDIT   : sentuhan pada tombol -> menggeser posisi tombol itu sendiri,
 *                   dipakai di layar "Atur Mapping Tombol".
 */
class FloatingButtonView(
    context: Context,
    var mapping: ButtonMapping,
    var editMode: Boolean = false,
    private val onMoved: ((ButtonMapping) -> Unit)? = null,
    private val onClickedInEdit: ((ButtonMapping) -> Unit)? = null
) : TextView(context) {

    private var toggledOn = false
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var totalMoveDistance = 0f

    init {
        text = mapping.label
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        textSize = 13f
        refreshBackground()
    }

    private fun refreshBackground() {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(if (editMode) 0x665FA8FF else 0x66FFFFFF)
        drawable.setStroke(3, if (toggledOn) Color.YELLOW else Color.parseColor("#CC1E88E5"))
        background = drawable
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editMode) {
            return handleEditTouch(event)
        }
        return handleGameTouch(event)
    }

    /** Mode edit: geser posisi tombol mengikuti jari, lalu simpan lewat callback. */
    private fun handleEditTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX; lastRawY = event.rawY
                totalMoveDistance = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastRawX
                val dy = event.rawY - lastRawY
                lastRawX = event.rawX; lastRawY = event.rawY
                totalMoveDistance += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                onMoved?.invoke(mapping.also {
                    // delta akan dikonversi ke persen oleh OverlayService/Editor pemanggil
                    it.x += dx / resources.displayMetrics.widthPixels
                    it.y += dy / resources.displayMetrics.heightPixels
                })
            }
            MotionEvent.ACTION_UP -> {
                // Hanya anggap "tap" (buka dialog edit) jika jari nyaris tidak bergeser
                if (totalMoveDistance < 12f) onClickedInEdit?.invoke(mapping)
            }
        }
        return true
    }

    /** Mode main: kirim perintah tap/hold/toggle ke Accessibility Service. */
    private fun handleGameTouch(event: MotionEvent): Boolean {
        val service = MobiAccessibilityService.instance ?: return true
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val tx = mapping.targetX * screenW
        val ty = mapping.targetY * screenH

        when (mapping.actionType) {
            ActionType.TAP -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    service.performTap(tx, ty)
                    alpha = 0.5f
                } else if (event.action == MotionEvent.ACTION_UP) {
                    alpha = 1f
                }
            }
            ActionType.HOLD -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { service.startHold(mapping.id, tx, ty); alpha = 0.5f }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { service.endHold(mapping.id); alpha = 1f }
                }
            }
            ActionType.TOGGLE -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    toggledOn = !toggledOn
                    if (toggledOn) service.startHold(mapping.id, tx, ty) else service.endHold(mapping.id)
                    refreshBackground()
                }
            }
        }
        return true
    }
}
