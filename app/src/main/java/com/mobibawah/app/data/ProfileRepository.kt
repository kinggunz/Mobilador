package com.mobibawah.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping
import com.mobibawah.app.model.MappingProfile

/**
 * Menyimpan mapping tiap game/aplikasi secara terpisah (per packageName) ke
 * SharedPreferences dalam bentuk JSON. Tidak butuh library tambahan.
 */
class ProfileRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("mobibawah_profiles", Context.MODE_PRIVATE)

    fun save(profile: MappingProfile) {
        prefs.edit().putString(profile.packageName, toJson(profile).toString()).apply()
    }

    fun load(packageName: String, defaultLabel: String): MappingProfile {
        val raw = prefs.getString(packageName, null)
            ?: return MappingProfile(packageName = packageName, appLabel = defaultLabel)
        return fromJson(JSONObject(raw))
    }

    fun hasProfile(packageName: String): Boolean = prefs.contains(packageName)

    fun delete(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }

    private fun toJson(p: MappingProfile): JSONObject {
        val obj = JSONObject()
        obj.put("packageName", p.packageName)
        obj.put("appLabel", p.appLabel)
        obj.put("touchpadEnabled", p.touchpadEnabled)
        obj.put("touchpadX", p.touchpadX)
        obj.put("touchpadY", p.touchpadY)
        obj.put("touchpadWidth", p.touchpadWidth)
        obj.put("touchpadHeight", p.touchpadHeight)
        obj.put("mouseSensitivity", p.mouseSensitivity)
        obj.put("hudImagePath", p.hudImagePath)
        val arr = JSONArray()
        p.buttons.forEach { b ->
            val bo = JSONObject()
            bo.put("id", b.id)
            bo.put("label", b.label)
            bo.put("keyName", b.keyName)
            bo.put("x", b.x)
            bo.put("y", b.y)
            bo.put("size", b.size)
            bo.put("targetX", b.targetX)
            bo.put("targetY", b.targetY)
            bo.put("actionType", b.actionType.name)
            arr.put(bo)
        }
        obj.put("buttons", arr)
        return obj
    }

    private fun fromJson(obj: JSONObject): MappingProfile {
        val buttons = mutableListOf<ButtonMapping>()
        val arr = obj.optJSONArray("buttons") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val bo = arr.getJSONObject(i)
            buttons.add(
                ButtonMapping(
                    id = bo.getString("id"),
                    label = bo.getString("label"),
                    keyName = bo.getString("keyName"),
                    x = bo.getDouble("x").toFloat(),
                    y = bo.getDouble("y").toFloat(),
                    size = bo.getDouble("size").toFloat(),
                    targetX = bo.getDouble("targetX").toFloat(),
                    targetY = bo.getDouble("targetY").toFloat(),
                    actionType = ActionType.valueOf(bo.getString("actionType"))
                )
            )
        }
        return MappingProfile(
            packageName = obj.getString("packageName"),
            appLabel = obj.optString("appLabel", ""),
            buttons = buttons,
            touchpadEnabled = obj.optBoolean("touchpadEnabled", true),
            touchpadX = obj.optDouble("touchpadX", 0.55).toFloat(),
            touchpadY = obj.optDouble("touchpadY", 0.55).toFloat(),
            touchpadWidth = obj.optDouble("touchpadWidth", 0.35).toFloat(),
            touchpadHeight = obj.optDouble("touchpadHeight", 0.30).toFloat(),
            mouseSensitivity = obj.optDouble("mouseSensitivity", 1.2).toFloat(),
            hudImagePath = if (obj.isNull("hudImagePath")) null else obj.optString("hudImagePath", null)
        )
    }
}
