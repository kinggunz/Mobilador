package com.mobibawah.app.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.MappingCodeUtil
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.model.ActionType
import com.mobibawah.app.model.ButtonMapping
import com.mobibawah.app.model.KeyCatalog
import com.mobibawah.app.model.MappingProfile
import com.mobibawah.app.overlay.FloatingButtonView
import java.io.File
import java.io.FileOutputStream

/**
 * Layar "Atur Mapping Tombol". Berjalan sebagai activity biasa (bukan
 * overlay window) sehingga tidak perlu izin SYSTEM_ALERT_WINDOW hanya
 * untuk mengedit — cukup pratinjau di dalam kotak previewArea.
 *
 * WAJIB landscape (dikunci lewat AndroidManifest): previewArea di sini
 * merepresentasikan layar game yang juga selalu landscape saat overlay
 * sungguhan berjalan, supaya posisi & ukuran yang kamu atur di sini persis
 * sama saat dipasang di atas game.
 *
 * Touchpad (area mouse-geser) otomatis muncul begitu layar ini dibuka
 * (tidak perlu ditambah manual) karena setiap game pada dasarnya bisa
 * memakai fitur geser-layar. Tap panel "V" di pojok kanan bawah untuk
 * sembunyikan/tampilkan bilah tombol pengaturan, supaya seluruh layar
 * bebas dipakai menaruh tombol di posisi mana pun (termasuk yang tadinya
 * ketutup bilah bawah).
 */
class MappingEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }

    private lateinit var previewArea: FrameLayout
    private lateinit var imgHud: ImageView
    private lateinit var repository: ProfileRepository
    private lateinit var profile: MappingProfile
    private var selectedMapping: ButtonMapping? = null
    private val buttonViews = HashMap<String, FloatingButtonView>()
    private var touchpadView: TouchpadEditView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleHudImagePicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapping_editor)
        title = getString(R.string.editor_title)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: "unknown"
        val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg
        repository = ProfileRepository(this)
        profile = repository.load(pkg, label)

        previewArea = findViewById(R.id.previewArea)

        // Klik KANAN mouse fisik di area kosong = langsung tawarkan tambah
        // tombol persis di titik itu (mirip alur cepat di app keymapper
        // sejenis) — klik kiri/sentuh jari tetap tidak berubah sama sekali.
        previewArea.setOnTouchListener { _, event ->
            val isRightClick = event.actionMasked == MotionEvent.ACTION_DOWN &&
                (event.buttonState and MotionEvent.BUTTON_SECONDARY) != 0
            if (isRightClick) {
                showQuickAddAt(event.x, event.y)
                true
            } else {
                false
            }
        }
        imgHud = findViewById(R.id.imgHud)
        val seekUkuran = findViewById<SeekBar>(R.id.seekUkuran)
        val btnTambah = findViewById<Button>(R.id.btnTambahTombol)
        val btnPreset = findViewById<Button>(R.id.btnPresetWasd)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanMapping)
        val btnPilihHud = findViewById<Button>(R.id.btnPilihHud)
        val btnHapusHud = findViewById<Button>(R.id.btnHapusHud)
        val btnBagikanKode = findViewById<Button>(R.id.btnBagikanKode)
        val btnImporKode = findViewById<Button>(R.id.btnImporKode)
        val panelBawah = findViewById<LinearLayout>(R.id.panelBawah)
        val btnToggleMenu = findViewById<TextView>(R.id.btnToggleMenu)
        val btnTutorial = findViewById<TextView>(R.id.btnTutorial)

        loadHudImageIfAny()

        // Tunggu previewArea selesai diukur baru gambar tombol & touchpad (butuh lebar/tinggi asli)
        previewArea.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                previewArea.viewTreeObserver.removeOnGlobalLayoutListener(this)
                redrawAllButtons()
                addTouchpadView() // touchpad selalu ada otomatis, tidak perlu ditambah manual
            }
        })

        btnToggleMenu.setOnClickListener {
            val show = panelBawah.visibility != View.VISIBLE
            panelBawah.visibility = if (show) View.VISIBLE else View.GONE
            btnToggleMenu.text = if (show) "▼" else "▲"
        }

        btnTutorial.setOnClickListener { showTutorialDialog() }

        btnPilihHud.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnHapusHud.setOnClickListener { clearHudImage() }
        btnBagikanKode.setOnClickListener { showShareCodeDialog() }
        btnImporKode.setOnClickListener { showImportCodeDialog() }

        btnTambah.setOnClickListener {
            KeyPickerDialog.show(this) { keyName ->
                val newMapping = ButtonMapping(label = keyName, keyName = keyName, x = 0.4f, y = 0.4f, targetX = 0.4f, targetY = 0.4f)
                profile.buttons.add(newMapping)
                addButtonView(newMapping)
            }
        }

        btnPreset.setOnClickListener {
            profile.buttons.clear()
            clearAllButtonViews()
            KeyCatalog.presetWASD().forEach {
                profile.buttons.add(it)
                addButtonView(it)
            }
            Toast.makeText(this, "Preset WASD diterapkan, geser sesuai posisi tombol di game-mu", Toast.LENGTH_LONG).show()
        }

        seekUkuran.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val mapping = selectedMapping ?: return
                if (!fromUser) return
                mapping.size = 0.04f + (progress / 100f) * 0.16f // rentang 4%-20% lebar layar
                resizeButtonView(mapping)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnSimpan.setOnClickListener {
            repository.save(profile)
            Toast.makeText(this, "Mapping untuk ${profile.appLabel} disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ---------- Klik kanan mouse: tambah tombol cepat di titik itu ----------

    private fun showQuickAddAt(pxX: Float, pxY: Float) {
        val previewW = previewArea.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val previewH = previewArea.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val fracX = (pxX / previewW).coerceIn(0f, 1f)
        val fracY = (pxY / previewH).coerceIn(0f, 1f)

        KeyPickerDialog.show(this) { keyName ->
            val newMapping = ButtonMapping(
                label = keyName, keyName = keyName,
                x = fracX, y = fracY, targetX = fracX, targetY = fracY
            )
            profile.buttons.add(newMapping)
            addButtonView(newMapping)
        }
    }

    // ---------- Tutorial ----------

    private fun showTutorialDialog() {
        val pesan = """
            DASAR
            • + TAMBAH TOMBOL: pilih key manual dari daftar, ATAU pilih "🎯 Deteksi Otomatis" lalu tekan langsung tombol fisiknya — otomatis terisi & jadi bukti keyboardmu terbaca.
            • KLIK KANAN MOUSE di area kosong: langsung tawarkan tambah tombol tepat di titik itu (cara cepat kalau kamu pakai mouse fisik untuk mengatur mapping).
            • PRESET WASD: langsung buat 4 tombol arah + Space + Shift, tinggal digeser ulang posisinya.
            • Tap tombol yang sudah ada = ganti nama key, ganti tipe aksi, atau hapus.

            TIPE AKSI TOMBOL
            • TAP: sekali sentuh sekali aksi.
            • HOLD: ditahan selama jari/tombol ditekan (cocok gerak jalan W/A/S/D).
            • TOGGLE: sekali tekan = nyala terus sampai ditekan lagi.
            • MACRO: sekali tekan = tap super cepat berulang otomatis (auto-tap) sampai ditekan lagi — tombolnya tetap bisa digeser & tidak mengganggu tombol/touchpad lain.

            KEYBOARD & MOUSE FISIK
            • Semua tombol di atas juga otomatis bisa dipicu lewat KEYBOARD FISIK (Bluetooth maupun kabel/OTG), asal Layanan Aksesibilitas sudah diaktifkan dan key-nya cocok dengan yang kamu atur di sini.
            • Mouse Bluetooth/USB: klik & drag umumnya sudah otomatis terbaca sistem seperti sentuhan biasa.

            TOUCHPAD (MOUSE-GESER)
            • Kotak biru "➤ MOUSE / GESER LAYAR" otomatis ada, tidak perlu ditambah manual. Geser untuk pindah posisi, tap sekali (tanpa geser) untuk buka pengaturan lebar/tinggi/sensitivitas/aktif-nonaktif.
            • Saat MAIN game: tap cepat di ikon panah tengah touchpad untuk nyala/matikan mode geser kapan saja.

            GAMBAR HUD & KODE MAPPING
            • PILIH GAMBAR HUD: opsional, ambil screenshot HUD game dari galeri sebagai acuan biar taruh tombolnya presisi.
            • BAGIKAN KODE: buat kode teks pendek dari layout tombolmu, salin & kirim ke teman.
            • IMPOR KODE: tempel kode dari teman untuk langsung memakai layout yang sama di game yang sedang kamu atur.

            LAIN-LAIN
            • Tombol ▲/▼ di atas panel: sembunyikan/tampilkan bilah menu ini, biar seluruh layar bebas dipakai naruh tombol.
            • Menu utama > Dashboard: cek keyboard/mouse apa saja yang sedang terhubung ke HP, plus tes langsung tekan tombol/gerak mouse untuk buktikan semuanya terbaca dengan benar.
            • Kalau keyboard fisik pernah berhenti berfungsi tiba-tiba: buka Pengaturan HP > Baterai > cari MobiladorWv1 > pilih "Tanpa batasan/Unrestricted" (bukan "Dioptimalkan"). Beberapa HP (Xiaomi/Oppo/Vivo dll) suka mematikan paksa Layanan Aksesibilitas demi hemat baterai.
            • Jangan lupa tekan SIMPAN setelah selesai mengatur.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Cara Pakai Mapping")
            .setMessage(pesan)
            .setPositiveButton("Mengerti", null)
            .show()
    }

    // ---------- Touchpad (otomatis ada, bisa digeser & diatur) ----------

    private fun addTouchpadView() {
        touchpadView?.let { previewArea.removeView(it) }
        val previewW = previewArea.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val previewH = previewArea.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

        val view = TouchpadEditView(
            this,
            onMoved = { dxFrac, dyFrac ->
                profile.touchpadX = (profile.touchpadX + dxFrac).coerceIn(0f, 1f - profile.touchpadWidth)
                profile.touchpadY = (profile.touchpadY + dyFrac).coerceIn(0f, 1f - profile.touchpadHeight)
                applyTouchpadPosition()
            },
            onTapped = { showTouchpadOptions() }
        )
        val params = FrameLayout.LayoutParams(
            (profile.touchpadWidth * previewW).toInt(),
            (profile.touchpadHeight * previewH).toInt(),
            Gravity.TOP or Gravity.START
        )
        params.leftMargin = (profile.touchpadX * previewW).toInt()
        params.topMargin = (profile.touchpadY * previewH).toInt()
        view.alpha = if (profile.touchpadEnabled) 1f else 0.35f
        previewArea.addView(view, 0, params) // index 0: di bawah tombol-tombol lain
        touchpadView = view
    }

    private fun applyTouchpadPosition() {
        val view = touchpadView ?: return
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = (profile.touchpadX * previewArea.width).toInt()
        params.topMargin = (profile.touchpadY * previewArea.height).toInt()
        view.layoutParams = params
    }

    private fun applyTouchpadSize() {
        val view = touchpadView ?: return
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.width = (profile.touchpadWidth * previewArea.width).toInt()
        params.height = (profile.touchpadHeight * previewArea.height).toInt()
        view.layoutParams = params
    }

    private fun showTouchpadOptions() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 0)
        }

        val switchAktif = Switch(this).apply {
            text = "Aktifkan touchpad saat main"
            isChecked = profile.touchpadEnabled
        }
        container.addView(switchAktif)

        container.addView(TextView(this).apply { text = "\nLebar" })
        val seekLebar = SeekBar(this).apply {
            max = 100
            progress = (((profile.touchpadWidth - 0.15f) / 0.5f) * 100f).toInt().coerceIn(0, 100)
        }
        container.addView(seekLebar)

        container.addView(TextView(this).apply { text = "Tinggi" })
        val seekTinggi = SeekBar(this).apply {
            max = 100
            progress = (((profile.touchpadHeight - 0.15f) / 0.5f) * 100f).toInt().coerceIn(0, 100)
        }
        container.addView(seekTinggi)

        container.addView(TextView(this).apply { text = "Sensitivitas Gerak" })
        val seekSensitif = SeekBar(this).apply {
            max = 100
            progress = (((profile.mouseSensitivity - 0.3f) / 2.7f) * 100f).toInt().coerceIn(0, 100)
        }
        container.addView(seekSensitif)

        seekLebar.setOnSeekBarChangeListener(simpleSeekListener { p ->
            profile.touchpadWidth = 0.15f + (p / 100f) * 0.5f
            applyTouchpadSize()
        })
        seekTinggi.setOnSeekBarChangeListener(simpleSeekListener { p ->
            profile.touchpadHeight = 0.15f + (p / 100f) * 0.5f
            applyTouchpadSize()
        })
        seekSensitif.setOnSeekBarChangeListener(simpleSeekListener { p ->
            profile.mouseSensitivity = 0.3f + (p / 100f) * 2.7f
        })
        switchAktif.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            profile.touchpadEnabled = checked
            touchpadView?.alpha = if (checked) 1f else 0.35f
        }

        AlertDialog.Builder(this)
            .setTitle("Atur Touchpad")
            .setView(container)
            .setPositiveButton("Selesai", null)
            .show()
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange(progress)
        }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }

    // ---------- Kode mapping (bagikan/impor ke orang lain) ----------

    /** Buat kode dari mapping saat ini, tampilkan di dialog dengan tombol Salin. */
    private fun showShareCodeDialog() {
        if (profile.buttons.isEmpty()) {
            Toast.makeText(this, "Belum ada tombol untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }
        val code = MappingCodeUtil.encode(
            buttons = profile.buttons,
            touchpadEnabled = profile.touchpadEnabled,
            touchpadX = profile.touchpadX, touchpadY = profile.touchpadY,
            touchpadWidth = profile.touchpadWidth, touchpadHeight = profile.touchpadHeight,
            mouseSensitivity = profile.mouseSensitivity
        )

        val input = EditText(this).apply {
            setText(code)
            isFocusable = false
            setPadding(24, 24, 24, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Kode Mapping Kamu (${code.length} karakter)")
            .setMessage("Kirim kode ini ke temanmu. Mereka tinggal tempel di tombol \"Impor Kode\" di game apa saja untuk memakai layout tombol yang sama persis.")
            .setView(input)
            .setPositiveButton("Salin") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Kode Mapping MobiladorWv1", code))
                Toast.makeText(this, "Kode disalin ke clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    /** Tempel kode dari orang lain, ganti mapping saat ini (dengan konfirmasi). */
    private fun showImportCodeDialog() {
        val input = EditText(this).apply {
            hint = "Tempel kode mapping di sini"
            setPadding(24, 24, 24, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Impor Kode Mapping")
            .setMessage("Ini akan MENGGANTI semua tombol yang sedang kamu atur di sini dengan layout dari kode tersebut.")
            .setView(input)
            .setPositiveButton("Pasang") { _, _ ->
                val imported = MappingCodeUtil.decode(input.text.toString())
                if (imported == null) {
                    Toast.makeText(this, "Kode tidak valid", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                profile.buttons.clear()
                profile.buttons.addAll(imported.buttons)
                profile.touchpadEnabled = imported.touchpadEnabled
                profile.touchpadX = imported.touchpadX
                profile.touchpadY = imported.touchpadY
                profile.touchpadWidth = imported.touchpadWidth
                profile.touchpadHeight = imported.touchpadHeight
                profile.mouseSensitivity = imported.mouseSensitivity
                clearAllButtonViews()
                profile.buttons.forEach { addButtonView(it) }
                addTouchpadView()
                Toast.makeText(this, "Mapping berhasil dipasang, jangan lupa Simpan", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ---------- Gambar HUD custom ----------

    private fun hudFileFor(pkg: String): File =
        File(filesDir, "hud_${pkg.replace(Regex("[^A-Za-z0-9_.]"), "_")}.png")

    private fun handleHudImagePicked(uri: Uri) {
        try {
            val outFile = hudFileFor(profile.packageName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            profile.hudImagePath = outFile.absolutePath
            imgHud.setImageURI(Uri.fromFile(outFile))
            Toast.makeText(this, "Gambar HUD dipasang sebagai acuan mapping", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadHudImageIfAny() {
        val path = profile.hudImagePath ?: return
        val file = File(path)
        if (file.exists()) imgHud.setImageURI(Uri.fromFile(file))
    }

    private fun clearHudImage() {
        profile.hudImagePath?.let { runCatching { File(it).delete() } }
        profile.hudImagePath = null
        imgHud.setImageDrawable(null)
    }

    // ---------- Tombol mapping ----------

    /** Hapus hanya tampilan tombol (bukan ImageView HUD / touchpad yang menempel permanen). */
    private fun clearAllButtonViews() {
        buttonViews.values.forEach { previewArea.removeView(it) }
        buttonViews.clear()
    }

    private fun redrawAllButtons() {
        clearAllButtonViews()
        profile.buttons.forEach { addButtonView(it) }
    }

    private fun addButtonView(mapping: ButtonMapping) {
        val previewW = previewArea.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val previewH = previewArea.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

        val view = FloatingButtonView(
            this, mapping, editMode = true,
            onMoved = { updated -> applyPosition(updated) },
            onClickedInEdit = { showButtonOptions(it) }
        )
        val sizePx = (mapping.size * previewW).toInt().coerceAtLeast(60)
        val params = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.TOP or Gravity.START)
        params.leftMargin = (mapping.x * previewW).toInt()
        params.topMargin = (mapping.y * previewH).toInt()
        previewArea.addView(view, params)
        buttonViews[mapping.id] = view
    }

    /** Dipanggil setiap kali FloatingButtonView digeser di mode edit. */
    private fun applyPosition(mapping: ButtonMapping) {
        mapping.x = mapping.x.coerceIn(0f, 1f)
        mapping.y = mapping.y.coerceIn(0f, 1f)
        mapping.targetX = mapping.x
        mapping.targetY = mapping.y
        val view = buttonViews[mapping.id] ?: return
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = (mapping.x * previewArea.width).toInt()
        params.topMargin = (mapping.y * previewArea.height).toInt()
        view.layoutParams = params
    }

    private fun resizeButtonView(mapping: ButtonMapping) {
        val view = buttonViews[mapping.id] ?: return
        val sizePx = (mapping.size * previewArea.width).toInt().coerceAtLeast(60)
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.width = sizePx
        params.height = sizePx
        view.layoutParams = params
    }

    /** Dialog opsi saat sebuah tombol di-tap di mode edit: ganti key, atur ukuran, atau hapus. */
    private fun showButtonOptions(mapping: ButtonMapping) {
        selectedMapping = mapping
        findViewById<SeekBar>(R.id.seekUkuran).progress =
            (((mapping.size - 0.04f) / 0.16f) * 100f).toInt().coerceIn(0, 100)

        AlertDialog.Builder(this)
            .setTitle("Tombol: ${mapping.label}")
            .setItems(arrayOf("Ganti Nama/Key", "Ganti Tipe Aksi (Tap/Hold/Toggle/Macro)", "Hapus Tombol")) { _, which ->
                when (which) {
                    0 -> KeyPickerDialog.show(this) { keyName ->
                        mapping.label = keyName
                        mapping.keyName = keyName
                        buttonViews[mapping.id]?.text = keyName
                    }
                    1 -> pickActionType(mapping)
                    2 -> deleteButton(mapping)
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun pickActionType(mapping: ButtonMapping) {
        val types = ActionType.values()
        val names = types.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tipe Aksi untuk ${mapping.label}")
            .setItems(names) { _, index -> mapping.actionType = types[index] }
            .show()
    }

    private fun deleteButton(mapping: ButtonMapping) {
        profile.buttons.remove(mapping)
        buttonViews[mapping.id]?.let { previewArea.removeView(it) }
        buttonViews.remove(mapping.id)
    }
}
