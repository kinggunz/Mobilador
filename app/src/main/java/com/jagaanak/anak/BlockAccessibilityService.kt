package com.jagaanak.anak

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import java.text.SimpleDateFormat
import java.util.*

class BlockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("jaga_anak", MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::prefs.isInitialized) prefs = getSharedPreferences("jaga_anak", MODE_PRIVATE)

        val packageName = event.packageName?.toString() ?: return
        val blockedPackage = prefs.getString("blockedPackage", "") ?: ""
        val sleepStart = prefs.getString("sleepStart", "") ?: ""
        val sleepEnd = prefs.getString("sleepEnd", "") ?: ""

        if (blockedPackage.isEmpty() || sleepStart.isEmpty() || sleepEnd.isEmpty()) return
        if (packageName != blockedPackage) return

        if (isWithinSleepTime(sleepStart, sleepEnd)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun isWithinSleepTime(start: String, end: String): Boolean {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val startCal = Calendar.getInstance().apply { time = fmt.parse(start) ?: return false }
        val endCal = Calendar.getInstance().apply { time = fmt.parse(end) ?: return false }
        val startMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)
        val endMinutes = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else {
            // Jam tidur melewati tengah malam, contoh 21:00 - 06:00
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }

    override fun onInterrupt() {}
}
