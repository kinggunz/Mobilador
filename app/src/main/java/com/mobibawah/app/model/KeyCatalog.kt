package com.mobibawah.app.model

/**
 * Daftar lengkap "nama tombol keyboard" yang bisa dipakai user sebagai label
 * mapping (huruf, angka, arah, dan tombol fungsi umum di game PC).
 * Ini murni daftar nama untuk UI picker — karena tanpa root, Mobibawah tidak
 * mengirim KeyEvent asli, melainkan memetakan tombol ini ke titik tap/hold
 * di layar (lihat ButtonMapping.targetX/targetY).
 */
object KeyCatalog {
    val HURUF = ('A'..'Z').map { it.toString() }
    val ANGKA = ('0'..'9').map { it.toString() }
    val ARAH = listOf("UP", "DOWN", "LEFT", "RIGHT")
    val FUNGSI = listOf(
        "SPACE", "SHIFT", "CTRL", "ALT", "TAB", "ENTER", "ESC",
        "F1", "F2", "F3", "F4", "F5",
        "MOUSE_LEFT", "MOUSE_RIGHT", "SCROLL_UP", "SCROLL_DOWN"
    )

    val SEMUA: List<String> = HURUF + ANGKA + ARAH + FUNGSI

    /** Preset cepat untuk game FPS/TPS umum: WASD + spasi (lompat) + shift (lari) */
    fun presetWASD(): List<ButtonMapping> = listOf(
        ButtonMapping(label = "W", keyName = "W", x = 0.14f, y = 0.62f, size = 0.09f, targetX = 0.14f, targetY = 0.62f, actionType = ActionType.HOLD),
        ButtonMapping(label = "A", keyName = "A", x = 0.05f, y = 0.72f, size = 0.09f, targetX = 0.05f, targetY = 0.72f, actionType = ActionType.HOLD),
        ButtonMapping(label = "S", keyName = "S", x = 0.14f, y = 0.82f, size = 0.09f, targetX = 0.14f, targetY = 0.82f, actionType = ActionType.HOLD),
        ButtonMapping(label = "D", keyName = "D", x = 0.23f, y = 0.72f, size = 0.09f, targetX = 0.23f, targetY = 0.72f, actionType = ActionType.HOLD),
        ButtonMapping(label = "SPACE", keyName = "SPACE", x = 0.85f, y = 0.80f, size = 0.10f, targetX = 0.85f, targetY = 0.80f, actionType = ActionType.TAP),
        ButtonMapping(label = "SHIFT", keyName = "SHIFT", x = 0.85f, y = 0.65f, size = 0.09f, targetX = 0.85f, targetY = 0.65f, actionType = ActionType.TOGGLE)
    )
}
