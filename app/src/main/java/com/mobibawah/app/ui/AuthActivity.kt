package com.mobibawah.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.AppPrefs

/**
 * Gerbang pertama aplikasi: buat akun lokal (username + password) sebelum
 * bisa memakai MobiladorWv1. Akun ini tersimpan di perangkat saja (lihat
 * AppPrefs) — bukan akun online. Setelah berhasil daftar, lanjut ke
 * TermsActivity (SK), lalu ke MainActivity dan tidak akan diminta lagi.
 */
class AuthActivity : AppCompatActivity() {

    private var isLoginMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val subtitle = findViewById<TextView>(R.id.txtAuthSubtitle)
        val inputUsername = findViewById<EditText>(R.id.inputUsername)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val inputConfirm = findViewById<EditText>(R.id.inputPasswordConfirm)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitAuth)

        isLoginMode = AppPrefs.isRegistered(this)
        applyMode(subtitle, inputConfirm, btnSubmit)

        btnSubmit.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString()

            if (username.length < 3) {
                Toast.makeText(this, "Username minimal 3 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 4) {
                Toast.makeText(this, "Password minimal 4 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                if (AppPrefs.checkLogin(this, username, password)) {
                    goNext()
                } else {
                    Toast.makeText(this, "Username atau password salah", Toast.LENGTH_SHORT).show()
                }
            } else {
                val confirm = inputConfirm.text.toString()
                if (password != confirm) {
                    Toast.makeText(this, "Konfirmasi password tidak sama", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AppPrefs.register(this, username, password)
                Toast.makeText(this, "Akun dibuat! Selamat datang, $username", Toast.LENGTH_SHORT).show()
                goNext()
            }
        }
    }

    private fun applyMode(subtitle: TextView, inputConfirm: EditText, btnSubmit: Button) {
        if (isLoginMode) {
            subtitle.text = "Masuk ke akunmu"
            inputConfirm.visibility = android.view.View.GONE
            btnSubmit.text = "MASUK"
        } else {
            subtitle.text = "Buat akun untuk mulai memakai"
            inputConfirm.visibility = android.view.View.VISIBLE
            btnSubmit.text = "DAFTAR & LANJUTKAN"
        }
    }

    /** Setelah akun siap: kalau SK belum disetujui, ke situ dulu; kalau sudah, langsung ke menu utama. */
    private fun goNext() {
        val intent = if (AppPrefs.isTermsAccepted(this)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, TermsActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
