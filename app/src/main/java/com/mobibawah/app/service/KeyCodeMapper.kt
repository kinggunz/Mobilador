package com.mobibawah.app.service

import android.view.KeyEvent

/**
 * Menerjemahkan kode tombol KEYBOARD FISIK (Bluetooth maupun kabel/OTG —
 * bagi Android keduanya sama saja, tampil sebagai KeyEvent standar) ke
 * nama key yang dipakai Mobibawah (lihat KeyCatalog), supaya satu mapping
 * yang sama bisa dipicu baik oleh tombol di layar MAUPUN tombol fisik.
 */
object KeyCodeMapper {

    private val map: Map<Int, String> = buildMap {
        put(KeyEvent.KEYCODE_A, "A"); put(KeyEvent.KEYCODE_B, "B")
        put(KeyEvent.KEYCODE_C, "C"); put(KeyEvent.KEYCODE_D, "D")
        put(KeyEvent.KEYCODE_E, "E"); put(KeyEvent.KEYCODE_F, "F")
        put(KeyEvent.KEYCODE_G, "G"); put(KeyEvent.KEYCODE_H, "H")
        put(KeyEvent.KEYCODE_I, "I"); put(KeyEvent.KEYCODE_J, "J")
        put(KeyEvent.KEYCODE_K, "K"); put(KeyEvent.KEYCODE_L, "L")
        put(KeyEvent.KEYCODE_M, "M"); put(KeyEvent.KEYCODE_N, "N")
        put(KeyEvent.KEYCODE_O, "O"); put(KeyEvent.KEYCODE_P, "P")
        put(KeyEvent.KEYCODE_Q, "Q"); put(KeyEvent.KEYCODE_R, "R")
        put(KeyEvent.KEYCODE_S, "S"); put(KeyEvent.KEYCODE_T, "T")
        put(KeyEvent.KEYCODE_U, "U"); put(KeyEvent.KEYCODE_V, "V")
        put(KeyEvent.KEYCODE_W, "W"); put(KeyEvent.KEYCODE_X, "X")
        put(KeyEvent.KEYCODE_Y, "Y"); put(KeyEvent.KEYCODE_Z, "Z")

        put(KeyEvent.KEYCODE_0, "0"); put(KeyEvent.KEYCODE_1, "1")
        put(KeyEvent.KEYCODE_2, "2"); put(KeyEvent.KEYCODE_3, "3")
        put(KeyEvent.KEYCODE_4, "4"); put(KeyEvent.KEYCODE_5, "5")
        put(KeyEvent.KEYCODE_6, "6"); put(KeyEvent.KEYCODE_7, "7")
        put(KeyEvent.KEYCODE_8, "8"); put(KeyEvent.KEYCODE_9, "9")

        put(KeyEvent.KEYCODE_DPAD_UP, "UP")
        put(KeyEvent.KEYCODE_DPAD_DOWN, "DOWN")
        put(KeyEvent.KEYCODE_DPAD_LEFT, "LEFT")
        put(KeyEvent.KEYCODE_DPAD_RIGHT, "RIGHT")

        put(KeyEvent.KEYCODE_SPACE, "SPACE")
        put(KeyEvent.KEYCODE_SHIFT_LEFT, "SHIFT")
        put(KeyEvent.KEYCODE_SHIFT_RIGHT, "SHIFT")
        put(KeyEvent.KEYCODE_CTRL_LEFT, "CTRL")
        put(KeyEvent.KEYCODE_CTRL_RIGHT, "CTRL")
        put(KeyEvent.KEYCODE_ALT_LEFT, "ALT")
        put(KeyEvent.KEYCODE_ALT_RIGHT, "ALT")
        put(KeyEvent.KEYCODE_TAB, "TAB")
        put(KeyEvent.KEYCODE_ENTER, "ENTER")
        put(KeyEvent.KEYCODE_ESCAPE, "ESC")
        put(KeyEvent.KEYCODE_F1, "F1"); put(KeyEvent.KEYCODE_F2, "F2")
        put(KeyEvent.KEYCODE_F3, "F3"); put(KeyEvent.KEYCODE_F4, "F4")
        put(KeyEvent.KEYCODE_F5, "F5")
    }

    /** Nama key Mobibawah untuk sebuah kode tombol fisik, atau null kalau tidak dikenal/tidak dipetakan. */
    fun nameFor(keyCode: Int): String? = map[keyCode]
}
