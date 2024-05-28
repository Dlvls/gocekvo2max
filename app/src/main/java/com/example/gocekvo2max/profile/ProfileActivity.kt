package com.example.gocekvo2max.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.R
import com.example.gocekvo2max.SettingActivity
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityProfileBinding
import com.example.gocekvo2max.history.ChartActivity
import java.text.SimpleDateFormat
import java.util.Date

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        navBottom()

        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, ProfileFormActivity::class.java)
            intent.putExtra(ProfileFormActivity.EDIT_TAG, true)
            startActivity(intent)
        }

        viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                if (user != null) {
                    binding.tvName.text = user.name
                    binding.tvCity.text = user.city.takeIf { it.isNotBlank() } ?: "-"
                    binding.tvJob.text = user.job.takeIf { it.isNotBlank() } ?: "-"

                    // Check and display weight, showing "-" if it's null or zero
                    val weightText = if (user.weight != null && user.weight != 0) {
                        "${user.weight} " + getString(R.string.kg)
                    } else {
                        "-"
                    }
                    binding.tvWeight.text = weightText

                    // Check and display age, showing "-" if it's null or zero
                    val ageText = if (user.age != null && user.age != 0) {
                        "${user.age} " + getString(R.string.years_old)
                    } else {
                        "-"
                    }
                    binding.tvAge.text = ageText

                    binding.tvEmail.text = user.email
                    binding.tvGender.text = user.gender ?: "-"
                    binding.tvAthlete.text =
                        if (user.athleticStat) getString(R.string.athlete) else getString(
                            R.string.non_athlete
                        )

                    val birthDate = user.birthDate?.let {
                        if (it != 0L) {
                            SimpleDateFormat("dd/MM/yyyy").format(Date(it))
                        } else {
                            "-"
                        }
                    } ?: "-"
                    binding.tvBirthdate.text = birthDate

                    Log.d(TAG, "userId: ${user.userId}")
                }
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
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.action_setting -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }
        binding.navView.selectedItemId = R.id.action_profile
    }

    companion object {
        const val TAG = "ProfileActivity"
    }
}