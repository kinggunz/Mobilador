package com.mobibawah.app.data

import android.content.Context

/**
 * Menyimpan SATU nilai kecil: package name game yang sedang "aktif" (lagi
 * dipakai lewat Mobilador). Ini bukan sekadar cache di memori — sengaja
 * ditulis ke SharedPreferences supaya BISA DIBACA ULANG oleh
 * MobiAccessibilityService kapan pun dia (re)connect, termasuk kalau
 * service-nya sempat mati/di-restart sendiri oleh Android (kehabisan
 * memori, dibatasi OEM tertentu seperti MIUI/ColorOS, dsb).
 *
 * Sebelumnya, mapping tombol HANYA dikirim ke Accessibility Service satu
 * kali lewat pemanggilan langsung (in-memory) saat overlay mulai. Kalau
 * saat itu service belum siap/baru restart, pemanggilan itu hilang begitu
 * saja dan TIDAK ADA cara service tahu harus muat mapping apa lagi —
 * inilah penyebab paling mungkin kenapa keyboard fisik tiba-tiba berhenti
 * berfungsi di sebagian perangkat. Dengan disimpan di sini, service bisa
 * membaca ulang sendiri dan pulih otomatis tanpa perlu OverlayService
 * memanggil apa pun lagi.
 */
object ActiveSessionPrefs {
    private const val PREF_NAME = "mobibawah_session"
    private const val KEY_TARGET_PACKAGE = "target_package"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setTargetPackage(context: Context, packageName: String) {
        prefs(context).edit().putString(KEY_TARGET_PACKAGE, packageName).apply()
    }

    fun getTargetPackage(context: Context): String? =
        prefs(context).getString(KEY_TARGET_PACKAGE, null)

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_TARGET_PACKAGE).apply()
    }
}
