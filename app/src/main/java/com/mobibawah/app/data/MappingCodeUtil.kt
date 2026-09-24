package com.mobibawah.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping

/**
 * Mengubah SATU set mapping tombol (+ pengaturan touchpad) menjadi kode
 * teks pendek (Base64) yang bisa dikirim lewat WhatsApp/dsb, lalu di-input
 * ulang oleh orang lain di aplikasinya sendiri untuk memakai layout yang
 * sama persis — tanpa perlu tahu ini game apa (kode berisi posisi/ukuran/
 * key/tipe aksi tombol saja, bukan nama aplikasi tertentu, supaya bisa
 * dipasang ke game apa pun yang sedang mereka atur).
 */
object MappingCodeUtil {

    private const val PREFIX = "MOBI1-" // penanda versi format kode

    fun encode(
        buttons: List<ButtonMapping>,
        touchpadEnabled: Boolean,
        touchpadX: Float, touchpadY: Float,
        touchpadWidth: Float, touchpadHeight: Float,
        mouseSensitivity: Float
    ): String {
        val root = JSONObject()
        root.put("touchpadEnabled", touchpadEnabled)
        root.put("touchpadX", touchpadX)
        root.put("touchpadY", touchpadY)
        root.put("touchpadWidth", touchpadWidth)
        root.put("touchpadHeight", touchpadHeight)
        root.put("mouseSensitivity", mouseSensitivity)
        val arr = JSONArray()
        buttons.forEach { b ->
            val bo = JSONObject()
            bo.put("label", b.label)
            bo.put("keyName", b.keyName)
            bo.put("x", b.x); bo.put("y", b.y)
            bo.put("size", b.size)
            bo.put("targetX", b.targetX); bo.put("targetY", b.targetY)
            bo.put("actionType", b.actionType.name)
            bo.put("macroIntervalMs", b.macroIntervalMs)
            arr.put(bo)
        }
        root.put("buttons", arr)
        val raw = Base64.encodeToString(root.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return PREFIX + raw
    }

    /** Hasil impor: daftar tombol baru + pengaturan touchpad, atau null kalau kode tidak valid. */
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
            val json = String(Base64.decode(body, Base64.NO_WRAP), Charsets.UTF_8)
            val root = JSONObject(json)
            val buttons = mutableListOf<ButtonMapping>()
            val arr = root.optJSONArray("buttons") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val bo = arr.getJSONObject(i)
                buttons.add(
                    ButtonMapping(
                        label = bo.getString("label"),
                        keyName = bo.getString("keyName"),
                        x = bo.getDouble("x").toFloat(),
                        y = bo.getDouble("y").toFloat(),
                        size = bo.getDouble("size").toFloat(),
                        targetX = bo.getDouble("targetX").toFloat(),
                        targetY = bo.getDouble("targetY").toFloat(),
                        actionType = ActionType.valueOf(bo.getString("actionType")),
                        macroIntervalMs = bo.optLong("macroIntervalMs", 80L)
                    )
                )
            }
            Imported(
                buttons = buttons,
                touchpadEnabled = root.optBoolean("touchpadEnabled", true),
                touchpadX = root.optDouble("touchpadX", 0.55).toFloat(),
                touchpadY = root.optDouble("touchpadY", 0.55).toFloat(),
                touchpadWidth = root.optDouble("touchpadWidth", 0.35).toFloat(),
                touchpadHeight = root.optDouble("touchpadHeight", 0.30).toFloat(),
                mouseSensitivity = root.optDouble("mouseSensitivity", 1.2).toFloat()
            )
        } catch (e: Exception) {
            null // kode rusak/tidak valid
        }
    }
}
