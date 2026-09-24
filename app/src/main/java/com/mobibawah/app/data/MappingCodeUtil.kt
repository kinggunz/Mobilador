package com.mobibawah.app.data

import android.util.Base64
import org.json.JSONArray
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping

/**
 * Mengubah SATU set mapping tombol (+ pengaturan touchpad) menjadi kode
 * teks SEPENDEK MUNGKIN yang gampang disalin & dikirim ke orang lain:
 *  - Struktur JSON pakai ARRAY (bukan object dengan nama field panjang).
 *  - Semua angka desimal dibulatkan 3 angka di belakang koma dulu supaya
 *    tidak muncul angka "sampah" akibat pembulatan Float->Double
 *    (mis. 0.6200000047683716 dipangkas jadi 0.62).
 *  - Tipe aksi disimpan sebagai angka index (0=TAP,1=HOLD,2=TOGGLE,3=MACRO),
 *    bukan teks "HOLD"/"TOGGLE" dst.
 *  - Base64 pakai NO_PADDING + NO_WRAP + URL_SAFE supaya tidak ada
 *    karakter '+', '/', '=' yang kadang bikin ribet saat disalin manual.
 */
object MappingCodeUtil {

    private const val PREFIX = "M1-" // penanda versi format kode, tetap singkat

    fun encode(
        buttons: List<ButtonMapping>,
        touchpadEnabled: Boolean,
        touchpadX: Float, touchpadY: Float,
        touchpadWidth: Float, touchpadHeight: Float,
        mouseSensitivity: Float
    ): String {
        val root = JSONArray()
        root.put(if (touchpadEnabled) 1 else 0)
        root.put(r3(touchpadX)); root.put(r3(touchpadY))
        root.put(r3(touchpadWidth)); root.put(r3(touchpadHeight))
        root.put(r3(mouseSensitivity))

        val btnArr = JSONArray()
        buttons.forEach { b ->
            val one = JSONArray()
            one.put(b.keyName)
            one.put(b.label)
            one.put(r3(b.x)); one.put(r3(b.y)); one.put(r3(b.size))
            one.put(r3(b.targetX)); one.put(r3(b.targetY))
            one.put(b.actionType.ordinal)
            one.put(b.macroIntervalMs)
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
                buttons.add(
                    ButtonMapping(
                        keyName = one.getString(0),
                        label = one.getString(1),
                        x = one.getDouble(2).toFloat(),
                        y = one.getDouble(3).toFloat(),
                        size = one.getDouble(4).toFloat(),
                        targetX = one.getDouble(5).toFloat(),
                        targetY = one.getDouble(6).toFloat(),
                        actionType = actions.getOrElse(one.getInt(7)) { ActionType.TAP },
                        macroIntervalMs = one.optLong(8, 80L)
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
            null // kode rusak/tidak valid
        }
    }

    /** Bulatkan ke 3 angka desimal supaya representasi Double-nya "bersih" (pendek). */
    private fun r3(value: Float): Double = Math.round(value * 1000.0) / 1000.0
}
