package com.jagaanak.ortu

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class MainActivity : AppCompatActivity() {

    private var pairingCode = ""
    private var installedApps = linkedMapOf<String, String>() // packageName -> label
    private var selectedPackage = ""
    private var sleepStart = "21:00"
    private var sleepEnd = "06:00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCode = findViewById<EditText>(R.id.etCode)
        val listView = findViewById<ListView>(R.id.listApps)
        val tvSleep = findViewById<TextView>(R.id.tvSleep)
        val btnStart = findViewById<Button>(R.id.btnSleepStart)
        val btnEnd = findViewById<Button>(R.id.btnSleepEnd)
        val btnSave = findViewById<Button>(R.id.btnSave)

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            pairingCode = etCode.text.toString().trim()
            if (pairingCode.isEmpty()) {
                Toast.makeText(this, "Masukkan kode pairing dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadInstalledApps(pairingCode, listView)
        }

        btnStart.setOnClickListener {
            showTimePicker { h, m ->
                sleepStart = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                tvSleep.text = "Jam tidur: $sleepStart - $sleepEnd"
            }
        }
        btnEnd.setOnClickListener {
            showTimePicker { h, m ->
                sleepEnd = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                tvSleep.text = "Jam tidur: $sleepStart - $sleepEnd"
            }
        }

        btnSave.setOnClickListener {
            if (pairingCode.isEmpty() || selectedPackage.isEmpty()) {
                Toast.makeText(this, "Pilih kode & aplikasi dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val settings = mapOf(
                "blockedPackage" to selectedPackage,
                "sleepStart" to sleepStart,
                "sleepEnd" to sleepEnd
            )
            FirebaseDatabase.getInstance().getReference("pairs/$pairingCode/settings")
                .setValue(settings)
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInstalledApps(code: String, listView: ListView) {
        FirebaseDatabase.getInstance().getReference("pairs/$code/installedApps")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    installedApps.clear()
                    for (child in snapshot.children) {
                        val label = child.child("label").getValue(String::class.java) ?: continue
                        val pkg = child.child("packageName").getValue(String::class.java) ?: continue
                        installedApps[pkg] = label
                    }

                    if (installedApps.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Belum ada data aplikasi. Pastikan kode benar dan HP anak sudah terhubung internet.",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    val labels = installedApps.values.toTypedArray()
                    val packages = installedApps.keys.toTypedArray()

                    listView.adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_single_choice,
                        labels
                    )
                    listView.choiceMode = ListView.CHOICE_MODE_SINGLE
                    listView.setOnItemClickListener { _, _, position, _ ->
                        selectedPackage = packages[position]
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showTimePicker(onSet: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, h, m -> onSet(h, m) },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show()
    }
}
