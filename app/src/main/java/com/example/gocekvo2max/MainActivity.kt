package com.example.gocekvo2max

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.balke.BalkeTestActivity
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityMainBinding
import com.example.gocekvo2max.history.ChartActivity
import com.example.gocekvo2max.maps.FinalBalkeTestActivity
import com.example.gocekvo2max.maps.FinalRockPortTestActivity
import com.example.gocekvo2max.profile.ProfileActivity
import com.example.gocekvo2max.rockport.RockPortTestActivity
import com.example.gocekvo2max.tutorial.balke.BTutorialActivity
import com.example.gocekvo2max.tutorial.rockport.RPTutorialActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: UserViewModel

    private val cameraPermissionCode = 101
    private val locationPermissionCode = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        Log.d(TAG, "credEmail: $userEmail, credPassword: $userPassword")

        requestCameraAndLocationPermissions()
        navBottom()
        intentActivity()

        getName(userEmail, userPassword)

        binding.tvName.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.cvBalkeTutorial.setOnClickListener {
            startActivity(Intent(this, BTutorialActivity::class.java))
        }

        binding.cvRockPortTutorial.setOnClickListener {
            startActivity(Intent(this, RPTutorialActivity::class.java))
        }
    }

    private fun intentActivity() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        binding.cvBalkeTest.setOnClickListener {
            if (checkPermissions()) {
                viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
                    .observe(this) { user ->
                        val weight = user?.weight
                        val age = user?.age
                        val gender = user?.gender

                        Log.d(TAG, "Balke: $weight, $age, $gender")

                        if (weight == null || age == null || gender.isNullOrEmpty() || weight == 0 || age < 0 || gender == "-") {
                            Toast.makeText(
                                this,
                                getString(R.string.please_fill_your_profile_correctly),
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this, ProfileActivity::class.java))
                        } else {
                            val intent = Intent(this, FinalBalkeTestActivity::class.java)
                            startActivity(intent)
                        }
                    }
            }
        }

        binding.cvRockPortTest.setOnClickListener {
            if (checkPermissions()) {
                val sharedPreferences =
                    getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
                val userEmail = sharedPreferences.getString("user_email", null)
                val userPassword = sharedPreferences.getString("user_password", null)

                viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
                    .observe(this) { user ->
                        val weight = user?.weight
                        val age = user?.age
                        val gender = user?.gender

                        Log.d(TAG, "RockPort: $weight, $age, $gender")

                        if (weight == null || age == null || gender.isNullOrEmpty() || weight == 0 || age < 0 || gender == "-") {
                            Toast.makeText(
                                this,
                                getString(R.string.please_fill_your_profile_correctly),
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this, ProfileActivity::class.java))
                        } else {
                            val intent = Intent(this, FinalRockPortTestActivity::class.java)
                            startActivity(intent)
                        }
                    }
            }
        }

        binding.roundedImageView.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val locationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!cameraPermissionGranted || !locationPermissionGranted) {
            Toast.makeText(
                this,
                getString(R.string.please_accept_the_required_permissions_for_location_and_camera_to_proceed),
                Toast.LENGTH_SHORT
            ).show()

            requestCameraAndLocationPermissions()

            return false
        }
        return true
    }

    private fun navBottom() {
        binding.navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
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
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }
        binding.navView.selectedItemId = R.id.action_home
    }

    private fun getName(email: String?, password: String?) {
        viewModel.getUserByEmailAndPassword(
            email.toString(),
            password.toString()
        ).observe(this) { user ->
            user?.let {
                binding.tvName.text = "Hi, ${user.name}"
            }
        }
    }

    private fun requestCameraAndLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            cameraPermissionCode -> {
                // Handle camera permission result
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, you can initialize or launch camera functionality
                } else {
                    // Handle camera permission denial
                }
            }

            locationPermissionCode -> {
                // Handle location permission result
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted, you can initialize or launch map functionality
                } else {
                    // Handle location permission denial
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}