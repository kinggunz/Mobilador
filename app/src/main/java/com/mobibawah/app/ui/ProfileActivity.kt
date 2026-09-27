package com.mobibawah.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.AppPrefs
import java.io.File
import java.io.FileOutputStream

/**
 * Halaman Profil: menampilkan avatar (bisa diganti dari galeri, disimpan
 * ke penyimpanan internal) dan username akun lokal, plus opsi ganti
 * password dan hapus akun. Menghapus akun TIDAK menghapus mapping game
 * yang sudah tersimpan (beda tempat penyimpanan) — cuma memaksa daftar
 * ulang di percobaan buka berikutnya.
 */
class ProfileActivity : AppCompatActivity() {

    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleAvatarPicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        findViewById<ImageButton>(R.id.btnBackProfil).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtUsernameProfil).text = AppPrefs.getUsername(this) ?: "-"

        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        loadAvatarIfAny(imgAvatar)
        imgAvatar.setOnClickListener { pickAvatarLauncher.launch("image/*") }

        findViewById<Button>(R.id.btnGantiPassword).setOnClickListener { showChangePasswordDialog() }
        findViewById<Button>(R.id.btnHapusAkun).setOnClickListener { confirmDeleteAccount() }
    }

    private fun avatarFile(): File = File(filesDir, "avatar_profil.png")

    private fun handleAvatarPicked(uri: Uri) {
        try {
            val outFile = avatarFile()
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            AppPrefs.setAvatarPath(this, outFile.absolutePath)
            findViewById<ImageView>(R.id.imgAvatar).setImageURI(Uri.fromFile(outFile))
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat avatar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadAvatarIfAny(imageView: ImageView) {
        val path = AppPrefs.getAvatarPath(this) ?: return
        val file = File(path)
        if (file.exists()) imageView.setImageURI(Uri.fromFile(file))
    }

    private fun showChangePasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        val inputLama = EditText(this).apply {
            hint = "Password saat ini"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val inputBaru = EditText(this).apply {
            hint = "Password baru"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(inputLama)
        container.addView(inputBaru)

        AlertDialog.Builder(this)
            .setTitle("Ganti Password")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val username = AppPrefs.getUsername(this) ?: return@setPositiveButton
                if (!AppPrefs.checkLogin(this, username, inputLama.text.toString())) {
                    Toast.makeText(this, "Password saat ini salah", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (inputBaru.text.toString().length < 4) {
                    Toast.makeText(this, "Password baru minimal 4 karakter", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                AppPrefs.changePassword(this, inputBaru.text.toString())
                Toast.makeText(this, "Password berhasil diganti", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Akun?")
            .setMessage("Kamu akan diminta daftar ulang saat membuka aplikasi lagi. Mapping game yang sudah kamu atur TIDAK akan hilang.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                AppPrefs.resetAccount(this)
                val intent = Intent(this, LauncherActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
