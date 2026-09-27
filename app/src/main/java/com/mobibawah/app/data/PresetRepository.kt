package com.mobibawah.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping

/**
 * Penyimpanan 3 SLOT PRESET mapping per game, masing-masing punya nama
 * sendiri (bebas diketik pengguna). Beda dengan mapping "aktif" yang
 * disimpan ProfileRepository (cuma satu per game, otomatis dipakai saat
 * MULAI), preset di sini adalah "cadangan tersimpan" yang bisa
 * di-load kapan saja untuk MENGGANTI mapping aktif — cocok buat
 * menyimpan beberapa gaya main berbeda (mis. "Agresif", "Sniper",
 * "Default") untuk game yang sama, tinggal pilih tanpa mengatur ulang
 * dari nol.
 */
object PresetRepository {

    private const val PREF_NAME = "mobibawah_presets"
    const val SLOT_COUNT = 3

    data class Preset(
        val name: String,
        val buttons: List<ButtonMapping>,
        val touchpadEnabled: Boolean,
        val touchpadX: Float,
        val touchpadY: Float,
        val touchpadWidth: Float,
        val touchpadHeight: Float,
        val mouseSensitivity: Float,
        val mouseSensitivityRight: Float
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun keyFor(packageName: String, slot: Int) = "${packageName}_slot$slot"

    /** Ambil isi slot (1..3) untuk game tertentu, atau null kalau slotnya masih kosong. */
    fun getSlot(context: Context, packageName: String, slot: Int): Preset? {
        val raw = prefs(context).getString(keyFor(packageName, slot), null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    /** Ambil semua 3 slot sekaligus (null di posisi yang masih kosong), buat ditampilkan di UI. */
    fun getAllSlots(context: Context, packageName: String): List<Preset?> =
        (1..SLOT_COUNT).map { getSlot(context, packageName, it) }

    fun saveSlot(
        context: Context,
        packageName: String,
        slot: Int,
        name: String,
        buttons: List<ButtonMapping>,
        touchpadEnabled: Boolean,
        touchpadX: Float,
        touchpadY: Float,
        touchpadWidth: Float,
        touchpadHeight: Float,
        mouseSensitivity: Float,
        mouseSensitivityRight: Float
    ) {
        val preset = Preset(
            name, buttons, touchpadEnabled, touchpadX, touchpadY,
            touchpadWidth, touchpadHeight, mouseSensitivity, mouseSensitivityRight
        )
        prefs(context).edit()
            .putString(keyFor(packageName, slot), toJson(preset).toString())
            .apply()
    }

    fun deleteSlot(context: Context, packageName: String, slot: Int) {
        prefs(context).edit().remove(keyFor(packageName, slot)).apply()
    }

    private fun toJson(p: Preset): JSONObject {
        val obj = JSONObject()
        obj.put("name", p.name)
        obj.put("touchpadEnabled", p.touchpadEnabled)
        obj.put("touchpadX", p.touchpadX)
        obj.put("touchpadY", p.touchpadY)
        obj.put("touchpadWidth", p.touchpadWidth)
        obj.put("touchpadHeight", p.touchpadHeight)
        obj.put("mouseSensitivity", p.mouseSensitivity)
        obj.put("mouseSensitivityRight", p.mouseSensitivityRight)
        val arr = JSONArray()
        p.buttons.forEach { b ->
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
        obj.put("buttons", arr)
        return obj
    }

    private fun fromJson(obj: JSONObject): Preset {
        val buttons = mutableListOf<ButtonMapping>()
        val arr = obj.optJSONArray("buttons") ?: JSONArray()
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
                    macroIntervalMs = bo.optLong("macroIntervalMs", 50L)
                )
            )
        }
        return Preset(
            name = obj.optString("name", "Preset"),
            buttons = buttons,
            touchpadEnabled = obj.optBoolean("touchpadEnabled", true),
            touchpadX = obj.optDouble("touchpadX", 0.55).toFloat(),
            touchpadY = obj.optDouble("touchpadY", 0.55).toFloat(),
            touchpadWidth = obj.optDouble("touchpadWidth", 0.35).toFloat(),
            touchpadHeight = obj.optDouble("touchpadHeight", 0.30).toFloat(),
            mouseSensitivity = obj.optDouble("mouseSensitivity", 1.2).toFloat(),
            mouseSensitivityRight = obj.optDouble("mouseSensitivityRight", 1.2).toFloat()
        )
    }
}
