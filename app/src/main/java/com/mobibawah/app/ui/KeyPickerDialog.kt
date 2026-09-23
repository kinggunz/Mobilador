package com.mobibawah.app.ui

import android.app.AlertDialog
import android.content.Context
import com.mobibawah.app.model.KeyCatalog

object KeyPickerDialog {
    fun show(context: Context, onPicked: (String) -> Unit) {
        val keys = KeyCatalog.SEMUA.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Pilih Tombol Keyboard")
            .setItems(keys) { _, index -> onPicked(keys[index]) }
            .setNegativeButton("Batal", null)
            .show()
    }
}
