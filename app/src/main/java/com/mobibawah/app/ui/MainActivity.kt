package com.mobibawah.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobibawah.app.R
import com.mobibawah.app.model.AppInfo
import com.mobibawah.app.overlay.OverlayService
import com.mobibawah.app.service.MobiAccessibilityService

class MainActivity : AppCompatActivity() {

    private var selectedApp: AppInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerApps)
        val btnOverlayPermission = findViewById<Button>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val btnAturMapping = findViewById<Button>(R.id.btnAturMapping)
        val btnMulai = findViewById<Button>(R.id.btnMulai)

        val apps = loadInstalledApps()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppListAdapter(apps) { selectedApp = it }

        btnOverlayPermission.setOnClickListener { requestOverlayPermission() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }

        btnAturMapping.setOnClickListener {
            val app = selectedApp
            if (app == null) {
                Toast.makeText(this, "Pilih aplikasi/game dulu", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, MappingEditorActivity::class.java)
                intent.putExtra(MappingEditorActivity.EXTRA_PACKAGE, app.packageName)
                intent.putExtra(MappingEditorActivity.EXTRA_LABEL, app.label)
                startActivity(intent)
            }
        }

        btnMulai.setOnClickListener { handleMulai() }
    }

    private fun handleMulai() {
        val app = selectedApp
        if (app == null) {
            Toast.makeText(this, "Pilih aplikasi/game dulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan overlay dulu (tombol di atas)", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Aktifkan Aksesibilitas Mobibawah dulu", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        // Semua izin siap -> jalankan overlay service, yang otomatis membuka game
        val serviceIntent = Intent(this, OverlayService::class.java)
        serviceIntent.putExtra(OverlayService.EXTRA_PACKAGE, app.packageName)
        serviceIntent.putExtra(OverlayService.EXTRA_LABEL, app.label)
        startForegroundService(serviceIntent)

        // Minimalkan Mobibawah supaya game tampil di depan
        moveTaskToBack(true)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        // Cek 1: instance service statis sudah terisi (paling akurat setelah connect)
        if (MobiAccessibilityService.instance != null) return true

        // Cek 2: cocokkan lewat daftar layanan aksesibilitas yang aktif di sistem
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "$packageName/${MobiAccessibilityService::class.java.name}"
        return enabledServices.split(":").any { it.equals(target, ignoreCase = true) }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        return resolved
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
