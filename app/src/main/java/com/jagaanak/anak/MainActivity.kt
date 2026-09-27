package com.jagaanak.anak

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("jaga_anak", MODE_PRIVATE)

        var code = prefs.getString("pairing_code", null)
        if (code == null) {
            code = (100000 + Random.nextInt(900000)).toString()
            prefs.edit().putString("pairing_code", code).apply()
        }

        findViewById<TextView>(R.id.tvCode).text = "Kode Pairing: $code"

        findViewById<Button>(R.id.btnAksesibilitas).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        uploadInstalledApps(code)
        listenForSettings(code)
    }

    private fun uploadInstalledApps(code: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .associate { info ->
                val safeKey = info.packageName.replace(".", "_")
                safeKey to mapOf(
                    "label" to pm.getApplicationLabel(info).toString(),
                    "packageName" to info.packageName
                )
            }

        FirebaseDatabase.getInstance().getReference("pairs/$code/installedApps")
            .setValue(apps)
    }

    private fun listenForSettings(code: String) {
        val ref = FirebaseDatabase.getInstance().getReference("pairs/$code/settings")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val blockedPackage = snapshot.child("blockedPackage").getValue(String::class.java) ?: ""
                val sleepStart = snapshot.child("sleepStart").getValue(String::class.java) ?: ""
                val sleepEnd = snapshot.child("sleepEnd").getValue(String::class.java) ?: ""

                prefs.edit()
                    .putString("blockedPackage", blockedPackage)
                    .putString("sleepStart", sleepStart)
                    .putString("sleepEnd", sleepEnd)
                    .apply()

                findViewById<TextView>(R.id.tvStatus).text =
                    "Aplikasi diblokir: $blockedPackage\nJam tidur: $sleepStart - $sleepEnd"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
