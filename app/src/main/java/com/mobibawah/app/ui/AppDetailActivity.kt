package com.mobibawah.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.overlay.OverlayService
import com.mobibawah.app.service.MobiAccessibilityService

/**
 * Halaman detail untuk SATU aplikasi/game yang dipilih dari AppPickerActivity:
 * menampilkan logo aplikasi besar dengan latar dari icon aplikasi itu
 * sendiri, status mapping (sudah/belum diatur), tombol "Atur Mapping
 * Tombol", dan tombol "MULAI" yang mengecek izin lalu menjalankan overlay
 * tepat di atas aplikasi tersebut.
 */
class AppDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }

    private lateinit var pkg: String
    private lateinit var label: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_detail)

        pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        label = intent.getStringExtra(EXTRA_LABEL) ?: pkg

        val icon = runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull()
        findViewById<ImageView>(R.id.imgAppIconBig).setImageDrawable(icon)
        findViewById<ImageView>(R.id.imgAppBackground).setImageDrawable(icon)
        findViewById<TextView>(R.id.txtAppLabel).text = label
        findViewById<ImageButton>(R.id.btnBackDetail).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnAturMappingDetail).setOnClickListener {
            val i = Intent(this, MappingEditorActivity::class.java)
            i.putExtra(MappingEditorActivity.EXTRA_PACKAGE, pkg)
            i.putExtra(MappingEditorActivity.EXTRA_LABEL, label)
            startActivity(i)
        }

        findViewById<Button>(R.id.btnMulaiDetail).setOnClickListener { handleMulai() }
    }

    override fun onResume() {
        super.onResume()
        val hasMapping = ProfileRepository(this).hasProfile(pkg)
        findViewById<TextView>(R.id.txtMappingStatus).text =
            if (hasMapping) "Mapping tombol sudah diatur" else "Mapping tombol belum diatur — atur dulu sebelum mulai"
    }

    private fun handleMulai() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan overlay dulu di pengaturan", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Aktifkan Layanan Aksesibilitas Mobibawah dulu", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        val serviceIntent = Intent(this, OverlayService::class.java)
        serviceIntent.putExtra(OverlayService.EXTRA_PACKAGE, pkg)
        serviceIntent.putExtra(OverlayService.EXTRA_LABEL, label)
        startForegroundService(serviceIntent)

        // Minimalkan Mobibawah supaya game tampil di depan
        moveTaskToBack(true)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (MobiAccessibilityService.instance != null) return true
        getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "$packageName/${MobiAccessibilityService::class.java.name}"
        return enabledServices.split(":").any { it.equals(target, ignoreCase = true) }
    }
}
