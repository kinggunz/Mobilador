package com.mobibawah.app.ui

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.service.KeyCodeMapper

/**
 * Halaman "Tes Game": WAJIB pilih satu tombol keyboard fisik (ditekan
 * manual, bukan dipilih dari daftar) sebelum bisa main. Karakter lari
 * otomatis, tombol yang dipilih dipakai untuk lompat melewati kaktus —
 * kalau kesentuh kaktus, mati.
 *
 * Ini murni alat bukti nyata: kalau lompatnya responsif tiap kamu pencet
 * tombol fisik itu, berarti Android di HP-mu memang benar-benar membaca
 * tombol itu dengan benar — jalur pembacaannya (KeyCodeMapper) SAMA
 * PERSIS dengan yang dipakai sistem mapping sungguhan di dalam game.
 */
class TestGameActivity : AppCompatActivity() {

    private var testKeyCode: Int? = null
    private lateinit var gameView: RunnerGameView
    private lateinit var txtSelectedKey: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_game)

        txtSelectedKey = findViewById(R.id.txtSelectedKey)
        val container = findViewById<FrameLayout>(R.id.gameContainer)

        gameView = RunnerGameView(this)
        container.addView(
            gameView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        gameView.onGameOver = { score ->
            runOnUiThread { Toast.makeText(this, "Mati! Skor akhir: $score", Toast.LENGTH_SHORT).show() }
        }

        findViewById<ImageButton>(R.id.btnBackTestGame).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGantiTombolTes).setOnClickListener { mintaPilihTombolBaru() }
    }

    private fun mintaPilihTombolBaru() {
        testKeyCode = null
        gameView.stopLoop()
        txtSelectedKey.text = "Tekan SATU tombol keyboard fisik untuk dites..."
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Tahap 1: belum ada tombol tes terpilih -> tombol PERTAMA yang
        // ditekan (dan dikenali) otomatis jadi tombol lompat untuk sesi ini.
        if (testKeyCode == null) {
            val nama = KeyCodeMapper.nameFor(keyCode)
            if (nama != null) {
                testKeyCode = keyCode
                txtSelectedKey.text = "Tombol tes: $nama — tekan lagi untuk LOMPAT!"
                gameView.onTestKeyPressed() // langsung mulai lari
            } else {
                Toast.makeText(this, "Tombol ini belum dikenali, coba tombol lain", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        // Tahap 2: tombol tes sudah terpilih -> setiap ditekan (tanpa
        // menghitung key-repeat) memicu lompat/mulai-ulang.
        if (keyCode == testKeyCode && event?.repeatCount == 0) {
            gameView.onTestKeyPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.stopLoop()
    }
}
