package com.mobibawah.app.ui

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.service.KeyCodeMapper

/**
 * Dashboard yang membuktikan secara langsung apakah keyboard & mouse fisik
 * (Bluetooth maupun USB/OTG — keduanya diperlakukan sama oleh Android)
 * benar-benar terbaca oleh sistem:
 *
 * 1. Daftar perangkat: memindai InputDevice yang sedang terhubung lewat
 *    InputManager, dan mendengarkan perubahan (colok/lepas) secara live
 *    lewat InputDeviceListener — otomatis refresh tanpa perlu keluar-masuk
 *    halaman.
 * 2. Tes langsung: menangkap KeyEvent (tombol fisik) dan MotionEvent
 *    bersumber mouse (SOURCE_MOUSE) SELAGI halaman ini terbuka, lalu
 *    menampilkan hasilnya apa adanya. Ini pakai jalur standar Android
 *    (onKeyDown/onGenericMotionEvent pada Activity), BUKAN lewat
 *    Accessibility Service — jadi ini murni bukti bahwa Android sendiri
 *    sudah mengenali perangkatmu, terlepas dari fitur mapping.
 */
class DashboardActivity : AppCompatActivity(), InputManager.InputDeviceListener {

    private lateinit var listPerangkat: LinearLayout
    private lateinit var txtLastKey: TextView
    private lateinit var txtMouseStatus: TextView
    private lateinit var inputManager: InputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        listPerangkat = findViewById(R.id.listPerangkat)
        txtLastKey = findViewById(R.id.txtLastKey)
        txtMouseStatus = findViewById(R.id.txtMouseStatus)
        inputManager = getSystemService(INPUT_SERVICE) as InputManager

        findViewById<ImageButton>(R.id.btnBackDashboard).setOnClickListener { finish() }

        refreshDeviceList()
    }

    override fun onResume() {
        super.onResume()
        inputManager.registerInputDeviceListener(this, null)
        refreshDeviceList()
    }

    override fun onPause() {
        super.onPause()
        inputManager.unregisterInputDeviceListener(this)
    }

    // ---------- Live listener colok/lepas perangkat ----------
    override fun onInputDeviceAdded(deviceId: Int) = refreshDeviceList()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshDeviceList()
    override fun onInputDeviceChanged(deviceId: Int) = refreshDeviceList()

    private fun refreshDeviceList() {
        listPerangkat.removeAllViews()
        val ids = InputDevice.getDeviceIds()
        var found = 0

        for (id in ids) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue // lewati perangkat virtual bawaan sistem

            val isKeyboard = device.supportsSource(InputDevice.SOURCE_KEYBOARD) &&
                device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            val isMouse = device.supportsSource(InputDevice.SOURCE_MOUSE) ||
                device.supportsSource(InputDevice.SOURCE_TOUCHPAD)

            if (!isKeyboard && !isMouse) continue
            found++

            val jenis = when {
                isKeyboard && isMouse -> "⌨ + 🖱 Keyboard & Mouse"
                isKeyboard -> "⌨ Keyboard"
                else -> "🖱 Mouse"
            }

            val row = TextView(this).apply {
                text = "$jenis\n${device.name}"
                setTextColor(resources.getColor(R.color.text_light, theme))
                textSize = 13f
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_input_field)
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 12
            listPerangkat.addView(row, params)
        }

        if (found == 0) {
            listPerangkat.addView(TextView(this).apply {
                text = "Belum ada keyboard/mouse eksternal terdeteksi. Sambungkan lewat Bluetooth atau OTG, halaman ini akan otomatis memperbarui."
                setTextColor(android.graphics.Color.parseColor("#888888"))
                textSize = 12f
            })
        }
    }

    // ---------- Tes langsung: keyboard fisik ----------
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0) {
            val nama = KeyCodeMapper.nameFor(keyCode) ?: KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            txtLastKey.text = "Terdeteksi: $nama"
        }
        return super.onKeyDown(keyCode, event)
    }

    // ---------- Tes langsung: mouse fisik (gerak/scroll) ----------
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
            txtMouseStatus.text = "Mouse bergerak — x=${event.x.toInt()}, y=${event.y.toInt()}"
        }
        return super.onGenericMotionEvent(event)
    }

    // ---------- Tes langsung: klik mouse fisik ----------
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE && ev.action == MotionEvent.ACTION_DOWN) {
            txtMouseStatus.text = "Klik mouse terdeteksi di x=${ev.x.toInt()}, y=${ev.y.toInt()}"
        }
        return super.dispatchTouchEvent(ev)
    }
}
