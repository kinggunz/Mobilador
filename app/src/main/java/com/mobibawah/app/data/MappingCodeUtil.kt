package com.mobibawah.app.data

import android.util.Base64
import org.json.JSONArray
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping

/**
 * Mengubah SATU set mapping tombol (+ pengaturan touchpad) menjadi kode
 * teks SEPENDEK MUNGKIN:
 *  - Struktur JSON pakai ARRAY (bukan object dengan nama field panjang).
 *  - Angka desimal dibulatkan 2 angka belakang koma (cukup presisi untuk
 *    posisi tombol, dan memangkas banyak karakter dibanding 3 desimal).
 *  - Tipe aksi & label disimpan seringkas mungkin: label HANYA disimpan
 *    kalau berbeda dari nama key (kasus paling umum: label == keyName,
 *    jadi tidak perlu disimpan dua kali).
 *  - macroIntervalMs HANYA disimpan kalau bukan nilai default (50ms).
 *  - Base64 URL_SAFE + NO_PADDING + NO_WRAP: tanpa karakter '+' '/' '='
 *    yang suka bikin ribet kalau disalin manual/lewat chat.
 */
object MappingCodeUtil {

    private const val PREFIX = "M1-"
    private const val DEFAULT_MACRO_MS = 50L

    fun encode(
        buttons: List<ButtonMapping>,
        touchpadEnabled: Boolean,
        touchpadX: Float, touchpadY: Float,
        touchpadWidth: Float, touchpadHeight: Float,
        mouseSensitivity: Float
    ): String {
        val root = JSONArray()
        root.put(if (touchpadEnabled) 1 else 0)
        root.put(r2(touchpadX)); root.put(r2(touchpadY))
        root.put(r2(touchpadWidth)); root.put(r2(touchpadHeight))
        root.put(r2(mouseSensitivity))

        val btnArr = JSONArray()
        buttons.forEach { b ->
            val one = JSONArray()
            one.put(b.keyName)
            one.put(if (b.label == b.keyName) "" else b.label) // "" = pakai keyName sebagai label
            one.put(r2(b.x)); one.put(r2(b.y)); one.put(r2(b.size))
            one.put(r2(b.targetX)); one.put(r2(b.targetY))
            one.put(b.actionType.ordinal)
            if (b.macroIntervalMs != DEFAULT_MACRO_MS) one.put(b.macroIntervalMs) // hanya kalau custom
            btnArr.put(one)
        }
        root.put(btnArr)

        val flags = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
        return PREFIX + Base64.encodeToString(root.toString().toByteArray(Charsets.UTF_8), flags)
    }

    data class Imported(
        val buttons: List<ButtonMapping>,
        val touchpadEnabled: Boolean,
        val touchpadX: Float, val touchpadY: Float,
        val touchpadWidth: Float, val touchpadHeight: Float,
        val mouseSensitivity: Float
    )

    fun decode(code: String): Imported? {
        return try {
            val trimmed = code.trim()
            val body = if (trimmed.startsWith(PREFIX)) trimmed.removePrefix(PREFIX) else trimmed
            val flags = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            val json = String(Base64.decode(body, flags), Charsets.UTF_8)
            val root = JSONArray(json)

            val actions = ActionType.values()
            val buttons = mutableListOf<ButtonMapping>()
            val btnArr = root.getJSONArray(6)
            for (i in 0 until btnArr.length()) {
                val one = btnArr.getJSONArray(i)
                val keyName = one.getString(0)
                val labelRaw = one.optString(1, "")
                buttons.add(
                    ButtonMapping(
                        keyName = keyName,
                        label = labelRaw.ifEmpty { keyName },
                        x = one.getDouble(2).toFloat(),
                        y = one.getDouble(3).toFloat(),
                        size = one.getDouble(4).toFloat(),
                        targetX = one.getDouble(5).toFloat(),
                        targetY = one.getDouble(6).toFloat(),
                        actionType = actions.getOrElse(one.getInt(7)) { ActionType.TAP },
                        macroIntervalMs = one.optLong(8, DEFAULT_MACRO_MS)
                    )
                )
            }
            Imported(
                buttons = buttons,
                touchpadEnabled = root.getInt(0) == 1,
                touchpadX = root.getDouble(1).toFloat(),
                touchpadY = root.getDouble(2).toFloat(),
                touchpadWidth = root.getDouble(3).toFloat(),
                touchpadHeight = root.getDouble(4).toFloat(),
                mouseSensitivity = root.getDouble(5).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Bulatkan ke 2 angka desimal supaya representasi Double-nya pendek & bersih. */
    private fun r2(value: Float): Double = Math.round(value * 100.0) / 100.0
}
