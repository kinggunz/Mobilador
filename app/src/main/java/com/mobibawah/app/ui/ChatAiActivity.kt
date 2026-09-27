package com.mobibawah.app.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Halaman Chat AI — memanggil layanan AI PIHAK KETIGA (bukan API resmi
 * dari provider AI manapun):
 *
 *   https://api.nexadev.my.id/ai/deepsek?text=<pesan yang di-encode>
 *
 * CATATAN JUJUR (baca sebelum pakai/ubah):
 *  - Ini API tidak resmi/tidak terverifikasi. Tidak ada jaminan selalu
 *    online, formatnya tidak berubah, atau data yang dikirim aman/tidak
 *    dicatat pihak ketiga. Jangan kirim data pribadi/sensitif lewat sini.
 *  - Karena saya (yang membuatkan kode ini) tidak punya akses internet
 *    untuk benar-benar memanggil API ini, saya TIDAK TAHU PERSIS bentuk
 *    JSON balasannya. Fungsi extractReply() di bawah sudah mencoba
 *    beberapa nama field yang UMUM dipakai API sejenis ("result",
 *    "message", "response", "answer", nested "data.*"), dan kalau semua
 *    itu tidak ketemu, akan menampilkan mentah-mentah teks balasannya
 *    supaya kamu tetap bisa lihat isinya dan gampang tahu field mana
 *    yang perlu ditambahkan di extractReply() kalau nama field aslinya
 *    ternyata beda.
 */
class ChatAiActivity : AppCompatActivity() {

    companion object {
        private const val BASE_URL = "https://api.nexadev.my.id/ai/deepsek"
    }

    private lateinit var rowChat: LinearLayout
    private lateinit var scrollChat: ScrollView
    private lateinit var progressChat: ProgressBar
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_ai)

        rowChat = findViewById(R.id.rowChat)
        scrollChat = findViewById(R.id.scrollChat)
        progressChat = findViewById(R.id.progressChat)
        val inputChat = findViewById<EditText>(R.id.inputChat)
        val btnKirim = findViewById<Button>(R.id.btnKirimChat)

        findViewById<ImageButton>(R.id.btnBackChat).setOnClickListener { finish() }

        addBubble("Halo! Tanya apa saja (chat ini pakai layanan AI pihak ketiga, bukan resmi).", isUser = false)

        btnKirim.setOnClickListener {
            val text = inputChat.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            addBubble(text, isUser = true)
            inputChat.setText("")
            sendToAi(text)
        }
    }

    private fun sendToAi(text: String) {
        progressChat.visibility = View.VISIBLE
        Thread {
            val result = runCatching { callApi(text) }
            mainHandler.post {
                progressChat.visibility = View.GONE
                result.onSuccess { reply -> addBubble(reply, isUser = false) }
                result.onFailure { e ->
                    addBubble(
                        "Gagal menghubungi AI (${e.message ?: "kesalahan tidak diketahui"}). " +
                            "API pihak ketiga ini bisa saja sedang down atau formatnya berubah.",
                        isUser = false, isError = true
                    )
                }
            }
        }.start()
    }

    /** Panggil API pihak ketiga lewat GET biasa. Dijalankan di background thread. */
    private fun callApi(text: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = URL("$BASE_URL?text=$encoded")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code: ${body.take(200)}")
            }
            return extractReply(body)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Coba ambil teks balasan dari beberapa kemungkinan bentuk JSON yang
     * umum dipakai API sejenis. Kalau tidak ketemu formatnya, kembalikan
     * teks mentahnya supaya tetap terlihat isinya.
     */
    private fun extractReply(raw: String): String {
        return try {
            val json = JSONObject(raw)
            val topLevelKeys = listOf("result", "message", "response", "answer", "reply", "output")
            for (key in topLevelKeys) {
                if (json.has(key) && !json.isNull(key)) return json.getString(key)
            }
            if (json.has("data")) {
                val data = json.get("data")
                if (data is JSONObject) {
                    for (key in topLevelKeys) {
                        if (data.has(key) && !data.isNull(key)) return data.getString(key)
                    }
                } else {
                    return data.toString()
                }
            }
            raw // format tidak dikenali -> tampilkan mentah
        } catch (e: Exception) {
            raw // bukan JSON sama sekali -> tampilkan apa adanya (mungkin memang teks polos)
        }
    }

    private fun addBubble(text: String, isUser: Boolean, isError: Boolean = false) {
        val bubble = TextView(this).apply {
            this.text = text
            setPadding(28, 20, 28, 20)
            textSize = 13f
            setTextColor(if (isError) Color.parseColor("#E57373") else Color.WHITE)
            setBackgroundResource(R.drawable.bg_input_field)
            if (isUser) backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.primary_dark, theme))
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = 16
        params.gravity = if (isUser) Gravity.END else Gravity.START
        val maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
        bubble.maxWidth = maxWidth
        rowChat.addView(bubble, params)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }
}
