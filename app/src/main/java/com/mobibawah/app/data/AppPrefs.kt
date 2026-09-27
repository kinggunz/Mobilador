package com.mobibawah.app.data

import android.content.Context
import java.security.MessageDigest

/**
 * Penyimpanan status akun & persetujuan Syarat & Ketentuan (SK).
 *
 * PENTING: Ini akun LOKAL di perangkat (tersimpan di penyimpanan aplikasi),
 * BUKAN akun online/cloud. Fungsinya sebagai gerbang "wajib daftar dulu"
 * sebelum aplikasi bisa dipakai, sesuai permintaan — bukan sistem login
 * internet sungguhan karena aplikasi ini tidak memiliki server backend.
 */
object AppPrefs {

    private const val PREF_NAME = "mobibawah_account"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD_HASH = "password_hash"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted"
    private const val KEY_AVATAR_PATH = "avatar_path"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isRegistered(context: Context): Boolean =
        prefs(context).contains(KEY_USERNAME)

    fun isTermsAccepted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TERMS_ACCEPTED, false)

    fun setTermsAccepted(context: Context) {
        prefs(context).edit().putBoolean(KEY_TERMS_ACCEPTED, true).apply()
    }

    fun register(context: Context, username: String, password: String) {
        prefs(context).edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD_HASH, hash(password))
            .apply()
    }

    fun getUsername(context: Context): String? = prefs(context).getString(KEY_USERNAME, null)

    fun getAvatarPath(context: Context): String? = prefs(context).getString(KEY_AVATAR_PATH, null)

    fun setAvatarPath(context: Context, path: String) {
        prefs(context).edit().putString(KEY_AVATAR_PATH, path).apply()
    }

    fun changePassword(context: Context, newPassword: String) {
        prefs(context).edit().putString(KEY_PASSWORD_HASH, hash(newPassword)).apply()
    }

    /** Hapus akun lokal (username/password/avatar) — TIDAK menghapus mapping game yang tersimpan. */
    fun resetAccount(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun checkLogin(context: Context, username: String, password: String): Boolean {
        val savedUser = prefs(context).getString(KEY_USERNAME, null) ?: return false
        val savedHash = prefs(context).getString(KEY_PASSWORD_HASH, null) ?: return false
        return savedUser.equals(username, ignoreCase = true) && savedHash == hash(password)
    }

    private fun hash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
