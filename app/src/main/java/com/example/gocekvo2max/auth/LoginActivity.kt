package com.example.gocekvo2max.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.R
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        binding.btnLogin.setOnClickListener {
            userLogin()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun userLogin() {
        val email = binding.edtEmail.text.toString()
        val password = binding.edtPassword.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.Main) {
                val userLiveData = viewModel.getUserByEmailAndPassword(email, password)

                userLiveData.observe(this@LoginActivity) { user ->
                    if (user != null) {
                        saveUserCredentials(email, password)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        showToast(getString(R.string.invalid_email_or_password))
                    }
                }
            }
        } else {
            showToast(getString(R.string.please_enter_email_and_password))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun saveUserCredentials(email: String, password: String) {
        val sharedPrefs = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("user_email", email)
        editor.putString("user_password", password)
        editor.apply()
    }

    companion object {
        const val TAG = "LoginActivity"
    }
}
