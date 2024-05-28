package com.example.gocekvo2max

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.gocekvo2max.auth.LoginActivity
import com.example.gocekvo2max.databinding.ActivitySettingBinding
import com.example.gocekvo2max.history.ChartActivity
import com.example.gocekvo2max.profile.ProfileActivity
import java.util.Locale

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("LANGUAGE_PREFERENCES", MODE_PRIVATE)

        val languageArray = resources.getStringArray(R.array.language_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    val editor = sharedPreferences.edit()
                    editor.putInt("LANGUAGE_INDEX", position)
                    editor.apply()

                    val languageCode = when (position) {
                        0 -> "en"
                        1 -> "id"
                        // Add more cases if you have additional languages
                        else -> "en" // Default to English if the position is out of bounds
                    }

                    // Update configuration and resources
                    val locale = Locale(languageCode)
                    Locale.setDefault(locale)

                    val configuration = Configuration()
                    configuration.locale = locale

                    val resources: Resources = resources
                    resources.updateConfiguration(configuration, resources.displayMetrics)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // No need to do anything here
                }
            }

        val languageIndex = sharedPreferences.getInt("LANGUAGE_INDEX", 0)
        binding.spinnerLanguage.setSelection(languageIndex)

        navBottom()

        binding.btnLogout.setOnClickListener {
            val editor = sharedPreferences.edit()

            editor.putString("user_email", null)
            editor.putString("user_password", null)

            editor.apply()

            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
        }
    }

    private fun navBottom() {
        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_history -> {
                    val intent = Intent(this, ChartActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_setting -> {
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }
        binding.navView.selectedItemId = R.id.action_setting
    }

    companion object {
        const val TAG = "SettingActivity"
    }

}