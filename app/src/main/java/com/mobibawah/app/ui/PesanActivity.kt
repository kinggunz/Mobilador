package com.mobibawah.app.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R

/**
 * Halaman "Pesan" — pesan panjang dari pembuat tentang proyek ini. Teksnya
 * ada di strings.xml (R.string.pesan_body) supaya gampang diedit/diganti
 * dengan cerita pribadimu sendiri kapan saja tanpa menyentuh kode.
 */
class PesanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pesan)

        findViewById<TextView>(R.id.txtPesanBody).text = getString(R.string.pesan_body)
        findViewById<ImageButton>(R.id.btnBackPesan).setOnClickListener { finish() }
    }
}
