package com.mobibawah.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Layanan Aksesibilitas: satu-satunya cara (tanpa root) untuk "menyentuh"
 * layar secara terprogram ke aplikasi lain di Android.
 *
 * Semua tombol HOLD/TOGGLE dan touchpad mouse memakai mekanisme yang sama:
 * "continuous touch" — satu sentuhan yang ditahan lalu disambung terus
 * menerus (GestureDescription.StrokeDescription.continueStroke) selama
 * jari pengguna masih menekan/menggeser di overlay, dan titiknya BISA
 * berpindah setiap saat. Ini yang membuat touchpad terasa seperti benar-
 * benar menggeser layar (drag asli), bukan sekadar tap berulang di titik
 * yang sama.
 *
 * - startTouch(id, x, y)   -> mulai menyentuh di titik (x,y).
 * - moveTouch(id, x, y)    -> pindahkan titik sentuh yang sedang ditahan
 *                              (dipanggil terus saat jari bergerak).
 * - endTouch(id)           -> lepas sentuhan (jari diangkat).
 * - performTap(x, y)       -> tap sekali+lepas, untuk tombol bertipe TAP.
 */
class MobiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MobiAccessibility"
        var instance: MobiAccessibilityService? = null
        private const val SEGMENT_MS = 120L // panjang tiap segmen stroke (semakin kecil = makin responsif)
    }

    private val activeStrokes = HashMap<String, GestureDescription.StrokeDescription>()
    // Titik target TERBARU untuk tiap id — dibaca ulang setiap kali sebuah
    // segmen selesai, supaya gerakan cepat (drag) tetap mengikuti jari.
    private val latestPoint = HashMap<String, Pair<Float, Float>>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Mobibawah Accessibility Service aktif")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Tap sekali di koordinat layar (px absolut) — untuk tombol TAP. */
    fun performTap(x: Float, y: Float, durationMs: Long = 60L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    /** Cepat: swipe A ke B sekali jalan (dipakai untuk efek kecil, bukan drag utama). */
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 30L) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    /** Mulai sentuhan yang ditahan di (x,y), diberi id unik (mis. id tombol atau "touchpad"). */
    fun startTouch(id: String, x: Float, y: Float) {
        latestPoint[id] = x to y
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, SEGMENT_MS, true)
        activeStrokes[id] = stroke
        dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (activeStrokes.containsKey(id)) continueTouch(id)
                }
            }, null
        )
    }

    /** Pindahkan titik sentuh yang sedang aktif (dipanggil berulang saat drag). */
    fun moveTouch(id: String, x: Float, y: Float) {
        if (!activeStrokes.containsKey(id)) {
            startTouch(id, x, y)
        } else {
            latestPoint[id] = x to y
        }
    }

    private fun continueTouch(id: String) {
        val prev = activeStrokes[id] ?: return
        val (x, y) = latestPoint[id] ?: return
        val path = Path().apply { moveTo(x, y) }
        val stroke = prev.continueStroke(path, 0, SEGMENT_MS, true)
        activeStrokes[id] = stroke
        dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (activeStrokes.containsKey(id)) continueTouch(id)
                }
            }, null
        )
    }

    /** Lepas sentuhan yang ditahan (jari diangkat dari tombol/touchpad). */
    fun endTouch(id: String) {
        val prev = activeStrokes.remove(id) ?: return
        val (x, y) = latestPoint.remove(id) ?: (0f to 0f)
        val path = Path().apply { moveTo(x, y) }
        val closing = prev.continueStroke(path, 0, 16L, false)
        dispatchGesture(GestureDescription.Builder().addStroke(closing).build(), null, null)
    }
}
