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
 * - performTap(x, y)            -> tap sekali, untuk tombol bertipe TAP.
 * - startHold(id, x, y)         -> mulai stroke tak-terputus (continueStroke),
 *                                   dipakai selama tombol HOLD sedang ditekan.
 * - continueHold(id, x, y)      -> perpanjang stroke yang sama (dipanggil berkala).
 * - endHold(id)                 -> lepas stroke saat jari diangkat dari tombol.
 * - performSwipe(x1,y1,x2,y2,ms)-> untuk gerakan mouse/kamera dari Touchpad.
 *
 * Catatan teknis: dispatchGesture menerima gesture dengan durasi terbatas
 * (maks sekitar 60 detik). Untuk HOLD yang lama, kita rangkai beberapa
 * StrokeDescription secara berurutan memakai continueStroke(prev, ..., true)
 * lalu di akhir dipanggil dengan willContinue=false untuk melepasnya.
 */
class MobiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MobiAccessibility"
        // instance statis supaya OverlayService bisa memanggil service ini
        var instance: MobiAccessibilityService? = null
        private const val SEGMENT_MS = 400L // panjang tiap segmen stroke saat hold
    }

    // Menyimpan stroke yang sedang berjalan per id tombol, agar bisa disambung/diakhiri
    private val activeStrokes = HashMap<String, GestureDescription.StrokeDescription>()

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

    /** Tap sekali di koordinat layar (px absolut). */
    fun performTap(x: Float, y: Float, durationMs: Long = 60L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    /** Mulai menahan sentuhan di (x,y) untuk tombol HOLD dengan id tertentu. */
    fun startHold(id: String, x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, SEGMENT_MS, true)
        activeStrokes[id] = stroke
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // Sambung otomatis selama tombol masih ditekan
                if (activeStrokes.containsKey(id)) continueHold(id, x, y)
            }
        }, null)
    }

    /** Menyambung stroke agar sentuhan tetap "ditahan" (dipanggil dari callback). */
    private fun continueHold(id: String, x: Float, y: Float) {
        val prev = activeStrokes[id] ?: return
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, SEGMENT_MS, true)
            .continueStroke(path, 0, SEGMENT_MS, true)
        activeStrokes[id] = stroke
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (activeStrokes.containsKey(id)) continueHold(id, x, y)
            }
        }, null)
    }

    /** Lepas sentuhan yang ditahan (jari diangkat dari tombol overlay). */
    fun endHold(id: String) {
        val prev = activeStrokes.remove(id) ?: return
        val path = Path().apply { moveTo(0f, 0f) }
        val closing = prev.continueStroke(path, 0, 20L, false)
        val gesture = GestureDescription.Builder().addStroke(closing).build()
        dispatchGesture(gesture, null, null)
    }

    /** Swipe cepat dari titik A ke titik B — dipakai touchpad untuk gerak kamera/mouse. */
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 30L) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
