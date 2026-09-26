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
import com.mobibawah.app.data.ActiveSessionPrefs
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.model.MappingProfile
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Service yang benar-benar menggambar tombol keyboard mengambang + touchpad
 * di atas game, menggunakan WindowManager (TYPE_APPLICATION_OVERLAY).
 *
 * Juga menyediakan SATU tombol bulat "mengambang" kecil (drag-handle) yang
 * selalu ada di layar untuk: sembunyikan/tampilkan semua tombol, atau
 * kembali ke MobiladorWv1 untuk mengubah mapping.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
        const val CHANNEL_ID = "mobibawah_overlay"
        const val NOTIF_ID = 1001
        // Dipakai MainActivity untuk tahu apakah overlay sedang berjalan,
        // supaya tombol "Matikan MobiladorWv1" tahu harus aktif atau tidak.
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

        val pkg = intent?.getStringExtra(EXTRA_PACKAGE) ?: "unknown"
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Game"

        // 1) Buka aplikasi/game target
        launchTargetApp(pkg)

        // 2) Muat mapping tersimpan untuk game ini
        val profile = ProfileRepository(this).load(pkg, label)

        // 2b) PENTING (perbaikan bug keyboard fisik): simpan package target
        // ke penyimpanan (bukan cuma variabel di memori) SUPAYA
        // MobiAccessibilityService bisa membaca & memuat ulang mapping-nya
        // SENDIRI kapan pun — baik langsung sekarang, maupun belakangan
        // kalau service itu sempat mati/restart di tengah jalan. Panggilan
        // langsung ke instance di bawah ini cuma untuk efek instan; yang
        // menjamin tetap benar walau ada gangguan adalah baris di atasnya.
        ActiveSessionPrefs.setTargetPackage(this, pkg)
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

    /**
     * Menggambar DUA window terpisah untuk fitur mouse-geser:
     *  1) "pad" — area drag sebenarnya. Mulai NONAKTIF (FLAG_NOT_TOUCHABLE)
     *     supaya di awal tidak mengganggu kontrol asli game (mis. tombol
     *     tembak FreeFire yang mungkin ada di bawahnya).
     *  2) "handle" — ikon kursor mouse kecil di tengah area itu, SELALU
     *     bisa disentuh (window terpisah, tidak pernah NOT_TOUCHABLE).
     *     Tap ikon ini: kursor HILANG (disembunyikan) dan window "pad"
     *     diaktifkan sehingga area itu bisa digeser bebas (mode mouse-
     *     look/geser peta ala FreeFire). Tap lagi: kursor muncul lagi,
     *     "pad" kembali tembus ke game.
     */
    private fun drawTouchpad(profile: MappingProfile) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val padW = (profile.touchpadWidth * screenW).toInt()
        val padH = (profile.touchpadHeight * screenH).toInt()
        val padX = (profile.touchpadX * screenW).toInt()
        val padY = (profile.touchpadY * screenH).toInt()

        val pad = TouchpadView(this).apply {
            sensitivityLeft = profile.mouseSensitivity
            sensitivityRight = profile.mouseSensitivityRight
        }
        val padParams = WindowManager.LayoutParams(
            padW, padH,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // mulai OFF: tembus ke game dulu
            PixelFormat.TRANSLUCENT
        )
        padParams.gravity = Gravity.TOP or Gravity.START
        padParams.x = padX
        padParams.y = padY
        windowManager.addView(pad, padParams)
        overlayViews.add(pad)

        val handleSize = 64
        val handle = TextView(this).apply {
            text = "➤"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.shape_overlay_button)
        }
        val handleParams = WindowManager.LayoutParams(
            handleSize, handleSize,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        handleParams.gravity = Gravity.TOP or Gravity.START
        handleParams.x = padX + padW / 2 - handleSize / 2
        handleParams.y = padY + padH / 2 - handleSize / 2

        var dragModeOn = false
        handle.setOnClickListener {
            dragModeOn = !dragModeOn
            if (dragModeOn) {
                // Kursor hilang, area drag diaktifkan -> game bisa digeser
                handle.visibility = View.INVISIBLE
                padParams.flags = padParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                // Kursor muncul lagi, area drag kembali tembus ke game
                handle.visibility = View.VISIBLE
                padParams.flags = padParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                // Jaga-jaga lepas stroke yang mungkin masih nyangkut di salah satu channel
                MobiAccessibilityService.instance?.endTouch(TouchpadView.TOUCH_ID_LEFT)
                MobiAccessibilityService.instance?.endTouch(TouchpadView.TOUCH_ID_RIGHT)
            }
            runCatching { windowManager.updateViewLayout(pad, padParams) }
        }

        windowManager.addView(handle, handleParams)
        overlayViews.add(handle)
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
                CHANNEL_ID, "MobiladorWv1 Overlay", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobiladorWv1 aktif")
            .setContentText("Tombol mapping sedang berjalan. Ketuk ikon ⌨ untuk sembunyikan.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        ActiveSessionPrefs.clear(this)
        MobiAccessibilityService.instance?.clearActiveMapping()
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
