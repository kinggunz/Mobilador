package com.mobibawah.app.model

/**
 * Jenis aksi yang dijalankan saat tombol overlay ditekan.
 * TAP      -> sekali tap di titik target (targetX, targetY) saat ditekan.
 * HOLD     -> tap ditahan (stroke kontinu) selama jari masih menekan tombol,
 *             cocok untuk gerakan W/A/S/D yang perlu "ditahan" di game.
 * TOGGLE   -> sekali tekan = tahan terus sampai ditekan lagi (mis. auto-run).
 */
enum class ActionType { TAP, HOLD, TOGGLE }

/**
 * Representasi satu tombol keyboard virtual mengambang di layar.
 *
 * x, y, size disimpan dalam PERSEN (0f..1f) terhadap lebar/tinggi layar,
 * supaya mapping tetap proporsional di berbagai ukuran layar / resolusi HP.
 *
 * targetX, targetY = titik pada layar game (dalam persen juga) yang akan
 * "disentuh" secara otomatis oleh Accessibility Service ketika tombol ini
 * ditekan. Biasanya diarahkan pas di atas tombol virtual game aslinya.
 */
data class ButtonMapping(
    var id: String = java.util.UUID.randomUUID().toString(),
    var label: String = "W",          // label yang tampil di tombol (mis. W A S D SPACE dsb)
    var keyName: String = "W",        // nama tombol keyboard fisik yang diwakili
    var x: Float = 0.1f,              // posisi tombol di layar (0..1)
    var y: Float = 0.7f,
    var size: Float = 0.08f,          // diameter tombol relatif terhadap lebar layar
    var targetX: Float = 0.1f,        // titik yang disentuh di layar game
    var targetY: Float = 0.7f,
    var actionType: ActionType = ActionType.HOLD
)

/**
 * Kumpulan mapping untuk satu aplikasi/game tertentu, dikenali dari packageName.
 */
data class MappingProfile(
    var packageName: String,
    var appLabel: String,
    var buttons: MutableList<ButtonMapping> = mutableListOf(),
    var touchpadEnabled: Boolean = true,   // pad untuk simulasi gerakan mouse/kamera
    var touchpadX: Float = 0.55f,
    var touchpadY: Float = 0.55f,
    var touchpadWidth: Float = 0.35f,
    var touchpadHeight: Float = 0.30f,
    var mouseSensitivity: Float = 1.2f
)
