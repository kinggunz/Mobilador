package com.mobibawah.app.ui

import android.app.AlertDialog
import android.content.Context
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import com.mobibawah.app.model.KeyCatalog
import com.mobibawah.app.service.KeyCodeMapper

object KeyPickerDialog {

    private const val OPSI_DETEKSI = "🎯 Deteksi Otomatis (tekan tombol fisik)"

    /**
     * Tampilkan daftar tombol untuk dipilih manual, ATAU pilih opsi paling
     * atas untuk deteksi otomatis: tekan tombol keyboard fisik sungguhan,
     * dan nama key-nya langsung terisi otomatis — sekaligus jadi bukti
     * nyata bahwa Mobilador membaca keyboard fisikmu dengan benar.
     */
    fun show(context: Context, onPicked: (String) -> Unit) {
        val items = (listOf(OPSI_DETEKSI) + KeyCatalog.SEMUA).toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Pilih Tombol Keyboard")
            .setItems(items) { _, index ->
                if (index == 0) showDetectDialog(context, onPicked) else onPicked(items[index])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDetectDialog(context: Context, onPicked: (String) -> Unit) {
        val message = TextView(context).apply {
            text = "Tekan SATU tombol di keyboard fisikmu sekarang...\n\n(Bluetooth maupun kabel/OTG sama-sama bisa)"
            setPadding(48, 32, 48, 32)
            textSize = 14f
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Menunggu Tombol Fisik")
            .setView(message)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnKeyListener { d, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val nama = KeyCodeMapper.nameFor(keyCode)
                if (nama != null) {
                    Toast.makeText(context, "Terdeteksi: $nama", Toast.LENGTH_SHORT).show()
                    onPicked(nama)
                    d.dismiss()
                } else {
                    Toast.makeText(context, "Tombol ini belum didukung, coba tombol lain", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
        dialog.show()
    }
}
