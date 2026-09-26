package com.mobibawah.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.overlay.OverlayService

/**
 * Menu utama (hub) MobiladorWv1:
 *  - Logo animasi (pulse) di header.
 *  - Tombol "+" besar di tengah -> buka AppPickerActivity untuk memilih game.
 *  - Baris "Game Tersimpan": game yang sudah pernah dipetakan, tap untuk
 *    langsung ke AppDetailActivity game itu.
 *  - Tombol "Matikan MobiladorWv1" untuk menghentikan overlay yang sedang berjalan.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animateLogo(findViewById(R.id.imgLogoMain))

        findViewById<Button>(R.id.btnTambahApp).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        findViewById<TextView>(R.id.btnOverlayPermission).setOnClickListener { requestOverlayPermission() }
        findViewById<TextView>(R.id.btnAccessibility).setOnClickListener { openAccessibilitySettings() }
        findViewById<TextView>(R.id.btnPesan).setOnClickListener {
            startActivity(Intent(this, PesanActivity::class.java))
        }
        findViewById<TextView>(R.id.btnProfil).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<TextView>(R.id.btnDashboard).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        findViewById<TextView>(R.id.btnMatikanMobi).setOnClickListener {
            if (OverlayService.isRunning) {
                stopService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "MobiladorWv1 dimatikan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "MobiladorWv1 belum berjalan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderFavoriteApps()
    }

    /** Animasi logo sederhana: membesar-mengecil pelan berulang (bernapas). */
    private fun animateLogo(view: ImageView) {
        val animator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f)
        val animatorY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f)
        listOf(animator, animatorY).forEach {
            it.duration = 1600
            it.repeatCount = ValueAnimator.INFINITE
            it.start()
        }
    }

    /** Tampilkan daftar game yang sudah pernah dipetakan sebagai ikon-ikon kecil. */
    private fun renderFavoriteApps() {
        val label = findViewById<TextView>(R.id.txtFavoritLabel)
        val scroll = findViewById<HorizontalScrollView>(R.id.scrollFavorit)
        val row = findViewById<LinearLayout>(R.id.rowFavorit)
        row.removeAllViews()

        val profiles = ProfileRepository(this).getAllProfiles()
        if (profiles.isEmpty()) {
            label.visibility = View.GONE
            scroll.visibility = View.GONE
            return
        }
        label.visibility = View.VISIBLE
        scroll.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        profiles.forEach { profile ->
            val itemView = inflater.inflate(R.layout.item_favorite_app, row, false)
            val icon = runCatching { packageManager.getApplicationIcon(profile.packageName) }.getOrNull()
            itemView.findViewById<ImageView>(R.id.imgFavIcon).setImageDrawable(icon)
            itemView.findViewById<TextView>(R.id.txtFavLabel).text = profile.appLabel
            itemView.setOnClickListener {
                val intent = Intent(this, AppDetailActivity::class.java)
                intent.putExtra(AppDetailActivity.EXTRA_PACKAGE, profile.packageName)
                intent.putExtra(AppDetailActivity.EXTRA_LABEL, profile.appLabel)
                startActivity(intent)
            }
            row.addView(itemView)
        }
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
