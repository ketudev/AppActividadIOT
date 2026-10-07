package com.ketudev.appactividadiot

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.ketudev.appactividadiot.databinding.ActivityMainBinding
import com.ketudev.appactividadiot.features.auth.LoginActivity
import com.ketudev.appactividadiot.features.luces.LucesActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "iot_app_prefs"
        private const val KEY_AUTO_OFF = "pref_auto_off"
        private const val KEY_LOCATION_POS = "pref_location_pos"
        private const val KEY_ALARM_TIME = "pref_alarm_time"
        private const val DEFAULT_ALARM_TIME = "23:00"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setupUserInfo()
        setupNavigation()
        setupPreferences()
        setupLogout()
    }

    private fun setupUserInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName?.ifBlank { "Usuario" } ?: "Usuario"
        binding.tvWelcome.text = getString(R.string.welcome_message, displayName)
    }

    private fun setupNavigation() {
        binding.btnManageLuces.setOnClickListener {
            val intent = Intent(this, LucesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupPreferences() {
        // Switch setup
        val isAutoOffEnabled = preferences.getBoolean(KEY_AUTO_OFF, false)
        binding.switchAutoOff.isChecked = isAutoOffEnabled
        binding.switchAutoOff.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(KEY_AUTO_OFF, isChecked).apply()
            val message = if (isChecked) "Apagado automático activado" else "Apagado automático desactivado"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Alarm Time setup
        val savedTime = preferences.getString(KEY_ALARM_TIME, DEFAULT_ALARM_TIME) ?: DEFAULT_ALARM_TIME
        binding.btnAlarmTime.text = "$savedTime hrs"
        binding.btnAlarmTime.setOnClickListener {
            showTimePicker()
        }

        // Spinner setup
        val locations = arrayOf(
            "Dormitorio Principal",
            "Dormitorio Infantil",
            "Dormitorio Visitas"
        )
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locations).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerLocation.adapter = spinnerAdapter

        val savedPosition = preferences.getInt(KEY_LOCATION_POS, 0)
        if (savedPosition in locations.indices) {
            binding.spinnerLocation.setSelection(savedPosition)
        }

        binding.spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isFirstSelection = true

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isFirstSelection) {
                    isFirstSelection = false
                    return
                }
                preferences.edit().putInt(KEY_LOCATION_POS, position).apply()
                Toast.makeText(this@MainActivity, "Ubicación preferida: ${locations[position]}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op
            }
        }
    }

    private fun showTimePicker() {
        val currentTime = preferences.getString(KEY_ALARM_TIME, DEFAULT_ALARM_TIME) ?: DEFAULT_ALARM_TIME
        val timeParts = currentTime.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 23
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText("Seleccionar hora de apagado")
            .build()

        picker.addOnPositiveButtonClickListener {
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
            preferences.edit().putString(KEY_ALARM_TIME, formattedTime).apply()
            binding.btnAlarmTime.text = "$formattedTime hrs"
            Toast.makeText(this, "Hora de apagado programada: $formattedTime hrs", Toast.LENGTH_SHORT).show()
        }

        picker.show(supportFragmentManager, "time_picker")
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}