package com.mobibawah.app.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.mobibawah.app.R
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.model.MappingProfile
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Service yang benar-benar menggambar tombol keyboard mengambang + touchpad
 * di atas game, menggunakan WindowManager (TYPE_APPLICATION_OVERLAY).
 *
 * Juga menyediakan SATU tombol bulat "mengambang" kecil (drag-handle) yang
 * selalu ada di layar untuk: sembunyikan/tampilkan semua tombol, atau
 * kembali ke Mobibawah untuk mengubah mapping.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
        const val CHANNEL_ID = "mobibawah_overlay"
        const val NOTIF_ID = 1001
        // Dipakai MainActivity untuk tahu apakah overlay sedang berjalan,
        // supaya tombol "Matikan Mobibawah" tahu harus aktif atau tidak.
        var isRunning: Boolean = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private var controlsVisible = true
    private lateinit var rootContainer: FrameLayout

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val pkg = intent?.getStringExtra(EXTRA_PACKAGE)
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Game"

        // 1) Buka aplikasi/game target
        pkg?.let { launchTargetApp(it) }

        // 2) Muat mapping tersimpan untuk game ini
        val profile = ProfileRepository(this).load(pkg ?: "unknown", label)

        // 2b) Daftarkan mapping ini ke Accessibility Service supaya KEYBOARD
        // FISIK (Bluetooth/kabel) juga bisa memicu aksi yang sama seperti
        // tombol di layar — ini bagian yang tadinya hilang & bikin keyboard
        // fisik tidak berfungsi sama sekali.
        MobiAccessibilityService.instance?.setActiveMapping(profile.buttons)

        // 3) Gambar semua tombol + touchpad + tombol mengambang kontrol
        drawButtons(profile)
        if (profile.touchpadEnabled) drawTouchpad(profile)
        drawFloatingHandle()

        return START_STICKY
    }

    private fun launchTargetApp(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent?.let { startActivity(it) }
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    private fun drawButtons(profile: MappingProfile) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        profile.buttons.forEach { mapping ->
            val btn = FloatingButtonView(this, mapping, editMode = false)
            val sizePx = (mapping.size * screenW).toInt()
            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = (mapping.x * screenW).toInt()
            params.y = (mapping.y * screenH).toInt()
            windowManager.addView(btn, params)
            overlayViews.add(btn)
        }
    }

    private fun drawTouchpad(profile: MappingProfile) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val pad = TouchpadView(this).apply { sensitivity = profile.mouseSensitivity }
        val params = WindowManager.LayoutParams(
            (profile.touchpadWidth * screenW).toInt(),
            (profile.touchpadHeight * screenH).toInt(),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = (profile.touchpadX * screenW).toInt()
        params.y = (profile.touchpadY * screenH).toInt()

        // Tap cepat (tanpa geser) di tengah touchpad = matikan/nyalakan mode
        // mouse-geser. Saat DIMATIKAN, window ditambah FLAG_NOT_TOUCHABLE
        // supaya sentuhan di area itu langsung "tembus" ke game di
        // bawahnya (bukan cuma berhenti menggeser, tapi benar tidak
        // menghalangi sama sekali) — persis seperti ikon panah nonaktif.
        pad.onToggleEnabled = { enabled ->
            params.flags = if (enabled) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            runCatching { windowManager.updateViewLayout(pad, params) }
        }

        windowManager.addView(pad, params)
        overlayViews.add(pad)
    }

    /** Tombol bulat kecil yang selalu bisa digeser & dipakai untuk show/hide semua tombol. */
    private fun drawFloatingHandle() {
        val handle = TextView(this).apply {
            text = "⌨"
            setBackgroundResource(R.drawable.shape_overlay_button)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            gravity = Gravity.CENTER
        }
        val params = WindowManager.LayoutParams(
            140, 140,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        var lastX = 0f; var lastY = 0f; var startX = 0; var startY = 0; var moved = false
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX; lastY = event.rawY
                    startX = params.x; startY = params.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager.updateViewLayout(handle, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleControlsVisibility()
                    true
                }
                else -> false
            }
        }
        windowManager.addView(handle, params)
        overlayViews.add(handle)
    }

    private fun toggleControlsVisibility() {
        controlsVisible = !controlsVisible
        val visibility = if (controlsVisible) View.VISIBLE else View.GONE
        // Index terakhir adalah handle itu sendiri, jadi selalu tetap terlihat
        overlayViews.dropLast(1).forEach { it.visibility = visibility }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Mobibawah Overlay", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mobibawah aktif")
            .setContentText("Tombol mapping sedang berjalan. Ketuk ikon ⌨ untuk sembunyikan.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        MobiAccessibilityService.instance?.clearActiveMapping()
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
