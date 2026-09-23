package com.mobibawah.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobibawah.app.R
import com.mobibawah.app.data.ProfileRepository
import com.mobibawah.app.model.ButtonMapping
import com.mobibawah.app.model.KeyCatalog
import com.mobibawah.app.model.MappingProfile
import com.mobibawah.app.overlay.FloatingButtonView

/**
 * Layar "Atur Mapping Tombol". Berjalan sebagai activity biasa (bukan
 * overlay window) sehingga tidak perlu izin SYSTEM_ALERT_WINDOW hanya
 * untuk mengedit — cukup pratinjau di dalam kotak previewArea yang
 * proporsinya diperlakukan seperti layar penuh.
 */
class MappingEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }

    private lateinit var previewArea: FrameLayout
    private lateinit var repository: ProfileRepository
    private lateinit var profile: MappingProfile
    private var selectedMapping: ButtonMapping? = null
    private var selectedView: FloatingButtonView? = null
    private val buttonViews = HashMap<String, FloatingButtonView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapping_editor)
        title = getString(R.string.editor_title)

        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: "unknown"
        val label = intent.getStringExtra(EXTRA_LABEL) ?: pkg
        repository = ProfileRepository(this)
        profile = repository.load(pkg, label)

        previewArea = findViewById(R.id.previewArea)
        val seekUkuran = findViewById<SeekBar>(R.id.seekUkuran)
        val btnTambah = findViewById<Button>(R.id.btnTambahTombol)
        val btnPreset = findViewById<Button>(R.id.btnPresetWasd)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanMapping)

        // Tunggu previewArea selesai diukur baru gambar tombol (butuh lebar/tinggi asli)
        previewArea.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                previewArea.viewTreeObserver.removeOnGlobalLayoutListener(this)
                redrawAllButtons()
            }
        })

        btnTambah.setOnClickListener {
            KeyPickerDialog.show(this) { keyName ->
                val newMapping = ButtonMapping(label = keyName, keyName = keyName, x = 0.4f, y = 0.4f, targetX = 0.4f, targetY = 0.4f)
                profile.buttons.add(newMapping)
                addButtonView(newMapping)
            }
        }

        btnPreset.setOnClickListener {
            profile.buttons.clear()
            previewArea.removeAllViews()
            buttonViews.clear()
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

    private fun redrawAllButtons() {
        previewArea.removeAllViews()
        buttonViews.clear()
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
        selectedView = buttonViews[mapping.id]
        findViewById<SeekBar>(R.id.seekUkuran).progress =
            (((mapping.size - 0.04f) / 0.16f) * 100f).toInt().coerceIn(0, 100)

        AlertDialog.Builder(this)
            .setTitle("Tombol: ${mapping.label}")
            .setItems(arrayOf("Ganti Nama/Key", "Ganti Tipe Aksi (Tap/Hold/Toggle)", "Hapus Tombol")) { _, which ->
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
        val types = com.mobibawah.app.model.ActionType.values()
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
