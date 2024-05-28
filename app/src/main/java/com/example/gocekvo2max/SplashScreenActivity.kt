package com.example.gocekvo2max

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.auth.LoginActivity
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivitySplashScreenBinding
import com.example.gocekvo2max.tutorial.rockport.RPTutorialActivity

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var viewModel: UserViewModel
    private val DURATION = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        Handler().postDelayed({
            viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
                .observe(this) { user ->
                    if (user != null) {
                        // User is logged in, go to main directly
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // User not logged in, go to login
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                }
        }, DURATION.toLong())

    }
}
