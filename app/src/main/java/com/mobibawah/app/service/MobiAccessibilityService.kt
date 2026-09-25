package com.mobibawah.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.mobibawah.app.data.ActiveSessionPrefs
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping

/**
 * Layanan Aksesibilitas: satu-satunya cara (tanpa root) untuk "menyentuh"
 * layar secara terprogram ke aplikasi lain di Android, DAN (fitur ini)
 * satu-satunya cara membaca tombol KEYBOARD FISIK (Bluetooth maupun
 * kabel/OTG — bagi Android engine-nya sama) walau fokus layar sedang ada
 * di game lain, lewat onKeyEvent() dengan canRequestFilterKeyEvents=true
 * (lihat accessibility_service_config.xml).
 *
 * PENTING — perbaikan bug "keyboard fisik tidak berfungsi": mapping yang
 * aktif TIDAK lagi hanya dikirim sekali secara langsung dari OverlayService.
 * Sekarang service ini SELF-HEALING: setiap kali dia (re)connect
 * (onServiceConnected) atau mendeteksi layar game yang jadi target sedang
 * di depan (onAccessibilityEvent -> TYPE_WINDOW_STATE_CHANGED), dia
 * membaca sendiri package target dari ActiveSessionPrefs dan memuat ulang
 * mapping-nya langsung dari penyimpanan. Jadi walau service ini sempat
 * mati/di-restart Android di tengah jalan (kejadian umum di sebagian HP),
 * dia otomatis pulih sendiri tanpa perlu tindakan apa pun dari pengguna.
 */
class MobiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MobiAccessibility"
        var instance: MobiAccessibilityService? = null
        private const val SEGMENT_MS = 120L
    }

    // ---------- Sentuhan (dipakai tombol layar & keyboard fisik) ----------
    private val activeStrokes = HashMap<String, GestureDescription.StrokeDescription>()
    private val latestPoint = HashMap<String, Pair<Float, Float>>()

    // ---------- Keyboard fisik ----------
    private var activeButtons: List<ButtonMapping> = emptyList()
    private var loadedForPackage: String? = null
    private val physicalToggleState = HashMap<String, Boolean>()
    private val macroHandler = Handler(Looper.getMainLooper())
    private val macroRunnables = HashMap<String, Runnable>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Mobibawah Accessibility Service aktif")
        reloadFromPersistedTarget() // pulihkan mapping kalau sebelumnya sempat aktif
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        clearActiveMapping()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val target = ActiveSessionPrefs.getTargetPackage(this) ?: return
            val foreground = event.packageName?.toString() ?: return
            if (foreground == target && loadedForPackage != target) {
                loadMappingFor(target)
            }
        }
    }

    override fun onInterrupt() {}

    /** Dipanggil OverlayService segera setelah mulai, untuk efek instan (tanpa menunggu event window). */
    fun setActiveMapping(buttons: List<ButtonMapping>) {
        activeButtons = buttons
        loadedForPackage = ActiveSessionPrefs.getTargetPackage(this)
    }

    fun clearActiveMapping() {
        activeButtons = emptyList()
        loadedForPackage = null
        physicalToggleState.clear()
        macroRunnables.keys.toList().forEach { stopMacroLoop(it) }
    }

    /** Baca ulang target dari penyimpanan & muat mapping-nya — dipanggil saat service baru (re)connect. */
    private fun reloadFromPersistedTarget() {
        val target = ActiveSessionPrefs.getTargetPackage(this) ?: return
        loadMappingFor(target)
    }

    private fun loadMappingFor(packageName: String) {
        val profile = ProfileRepository(this).load(packageName, packageName)
        activeButtons = profile.buttons
        loadedForPackage = packageName
        Log.i(TAG, "Mapping dimuat untuk $packageName: ${activeButtons.size} tombol")
    }

    // ---------- Keyboard fisik: inti perbaikan mapping ----------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyName = KeyCodeMapper.nameFor(event.keyCode) ?: return super.onKeyEvent(event)
        val mapping = activeButtons.find { it.keyName.equals(keyName, ignoreCase = true) }
            ?: return super.onKeyEvent(event) // key tidak dipetakan -> biarkan lewat normal

        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        val tx = mapping.targetX * screenW
        val ty = mapping.targetY * screenH
        val isFirstDown = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        val isUp = event.action == KeyEvent.ACTION_UP

        when (mapping.actionType) {
            ActionType.TAP -> if (isFirstDown) performTap(tx, ty)
            ActionType.HOLD -> {
                if (isFirstDown) startTouch(mapping.id, tx, ty)
                if (isUp) endTouch(mapping.id)
            }
            ActionType.TOGGLE -> if (isFirstDown) {
                val on = !(physicalToggleState[mapping.id] ?: false)
                physicalToggleState[mapping.id] = on
                if (on) startTouch(mapping.id, tx, ty) else endTouch(mapping.id)
            }
            ActionType.MACRO -> if (isFirstDown) {
                val on = !(physicalToggleState[mapping.id] ?: false)
                physicalToggleState[mapping.id] = on
                if (on) startMacroLoop(mapping, tx, ty) else stopMacroLoop(mapping.id)
            }
        }
        return true // konsumsi event supaya tidak "bocor" ganda ke game
    }

    private fun startMacroLoop(mapping: ButtonMapping, tx: Float, ty: Float) {
        stopMacroLoop(mapping.id)
        val runnable = object : Runnable {
            override fun run() {
                if (physicalToggleState[mapping.id] != true) return
                performTap(tx, ty)
                macroHandler.postDelayed(this, mapping.macroIntervalMs)
            }
        }
        macroRunnables[mapping.id] = runnable
        macroHandler.post(runnable)
    }

    private fun stopMacroLoop(id: String) {
        macroRunnables.remove(id)?.let { macroHandler.removeCallbacks(it) }
    }

    // ---------- Gesture dasar (dipakai FloatingButtonView & TouchpadView juga) ----------

    fun performTap(x: Float, y: Float, durationMs: Long = 60L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 30L) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

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

    fun moveTouch(id: String, x: Float, y: Float) {
        if (!activeStrokes.containsKey(id)) startTouch(id, x, y) else latestPoint[id] = x to y
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

    fun endTouch(id: String) {
        val prev = activeStrokes.remove(id) ?: return
        val (x, y) = latestPoint.remove(id) ?: (0f to 0f)
        val path = Path().apply { moveTo(x, y) }
        val closing = prev.continueStroke(path, 0, 16L, false)
        dispatchGesture(GestureDescription.Builder().addStroke(closing).build(), null, null)
    }
}
