package com.mobibawah.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.data.AppPrefs

/**
 * Titik masuk aplikasi (launcher). Tidak punya tampilan sendiri — cuma
 * memutuskan halaman pertama yang harus dilihat pengguna:
 *   belum punya akun      -> AuthActivity (daftar)
 *   akun ada, SK belum ok -> TermsActivity
 *   semua sudah lengkap   -> MainActivity (menu utama)
 */
class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val next = when {
            !AppPrefs.isRegistered(this) -> AuthActivity::class.java
            !AppPrefs.isTermsAccepted(this) -> TermsActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
