package com.example.gocekvo2max.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.R
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityProfileFormBinding
import java.util.Calendar

class ProfileFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileFormBinding
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val isEdit = intent.getBooleanExtra(EDIT_TAG, false)

        viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                Log.d(TAG, "$user")
            }

        initializeForEdit()
        spinnerImplementation()
    }

    private fun initializeForEdit() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        viewModel.getUserByEmailAndPassword(
            userEmail.toString(),
            userPassword.toString()
        ).observeOnce { user ->
            if (user != null) {
                binding.edtName.setText(user.name)
                binding.edtJob.setText(user.job)
                binding.edtCity.setText(user.city)

                if (user.age != null && user.weight != null && user.weight != 0) {
                    binding.edtAge.setText(user.age.toString())
                    binding.edtWeight.setText(user.weight.toString())
                } else {
                    binding.edtAge.setText("")
                    binding.edtWeight.setText("")
                }

                if (user.gender.equals("Male", ignoreCase = true)) {
                    binding.rbMale.isChecked = true
                } else if (user.gender.equals("Female", ignoreCase = true)) {
                    binding.rbFemale.isChecked = true
                }
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = user.birthDate ?: System.currentTimeMillis()
                binding.dateBirthPicker.init(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    null
                )
            }
        }

        binding.btnSave.setOnClickListener {
            val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("user_email", null)
            val userPassword = sharedPreferences.getString("user_password", null)

            viewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
                .observeOnce { existingUser ->
                    val updatedUser = existingUser?.copy(
                        name = binding.edtName.text.toString(),
                        job = binding.edtJob.text.toString(),
                        city = binding.edtCity.text.toString(),
                        birthDate = getSelectedDateFromDatePicker(binding.dateBirthPicker),
                        age = binding.edtAge.text.toString().takeIf { it.isNotBlank() }
                            ?.toIntOrNull(),
                        weight = binding.edtWeight.text.toString().takeIf { it.isNotBlank() }
                            ?.toIntOrNull(),
                        athleticStat = getSelectedAthleticState(binding.spinnerAthlete),
                        gender = getSelectedGender(binding.radioGroupGender)
                    )

                    updatedUser?.let {
                        viewModel.updateUserProfile(it)
                        Log.d(TAG, "User data updated: $it")
                        startActivity(Intent(this@ProfileFormActivity, MainActivity::class.java))
                    }
                }
        }
    }

    private fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
        observeForever(object : Observer<T> {
            override fun onChanged(value: T) {
                observer(value)
                removeObserver(this)
            }
        })
    }

    private fun getSelectedDateFromDatePicker(datePicker: DatePicker): Long {
        val calendar = Calendar.getInstance()
        calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
        return calendar.timeInMillis
    }

    private fun getSelectedAthleticState(spinner: Spinner): Boolean {
        val selectedValue = spinner.selectedItem.toString()
        return selectedValue == "Yes" // You can adjust this based on your spinner items
    }

    private fun spinnerImplementation() {
        val spinner = binding.spinnerAthlete
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.athlete_options,  // Assuming you have an array resource with options
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun getSelectedGender(radioGroup: RadioGroup): String {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.rbFemale -> "Female"
            R.id.rbMale -> "Male"
            else -> "-"
        }
    }

    companion object {
        const val TAG = "ProfileFillingActivity"
        const val EDIT_TAG = "EDIT_PERMISSION"
    }
}