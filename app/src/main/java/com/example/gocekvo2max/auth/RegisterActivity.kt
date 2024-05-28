package com.example.gocekvo2max.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.R
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun registerUser() {
        val name = binding.edtName.text.toString()
        val email = binding.edtEmail.text.toString()
        val password = binding.edtPassword.text.toString()

        if (name.isEmpty() or email.isEmpty() or password.isEmpty()) {
            showToast(getString(R.string.all_fields_must_be_filled))
        } else {
            val emailPattern = Pattern.compile("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")
            val passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}\$")

            if (!emailPattern.matcher(email).matches()) {
                showToast(getString(R.string.invalid_email_type))
            } else if (!passwordPattern.matcher(password).matches()) {
                showToast(getString(R.string.password_must_have_at_least_6_characters_with_at_least_1_number))
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    // Retrieve the user by email
                    val userLiveData = viewModel.getUserByEmail(email)

                    // Observe the user data
                    userLiveData.observeOnce(this@RegisterActivity) { user ->
                        if (user != null) {
                            showToast(getString(R.string.email_already_in_use))
                        } else {
                            viewModel.insertUser(name, "", 0, "", email, password, 0, 0, false)
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        }
                    }
                }
            }
        }
    }

    private fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: Observer<T>) {
        observe(owner, object : Observer<T> {
            override fun onChanged(t: T) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }

    companion object {
        const val TAG = "RegisterActivity"
    }
}