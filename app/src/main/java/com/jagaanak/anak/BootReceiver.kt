package com.jagaanak.anak

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Layanan Aksesibilitas otomatis aktif lagi oleh sistem setelah reboot
        // selama izinnya belum dicabut manual oleh pengguna di menu Setelan.
    }
}
