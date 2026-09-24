package com.mobibawah.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.AppPrefs

/** Halaman SK (Syarat & Ketentuan) — wajib dicentang sebelum lanjut ke menu utama. */
class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        findViewById<TextView>(R.id.txtTermsBody).text = getString(R.string.terms_body)

        val checkbox = findViewById<CheckBox>(R.id.checkAgreeTerms)
        val btnAgree = findViewById<Button>(R.id.btnAgreeTerms)

        checkbox.setOnCheckedChangeListener { _, isChecked -> btnAgree.isEnabled = isChecked }

        btnAgree.setOnClickListener {
            AppPrefs.setTermsAccepted(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
