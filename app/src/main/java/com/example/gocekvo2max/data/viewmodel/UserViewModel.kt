package com.example.gocekvo2max.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.gocekvo2max.data.database.entity.UserEntity
import com.example.gocekvo2max.data.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    fun insertUser(
        name: String,
        city: String,
        birthDate: Long,
        job: String,
        email: String,
        password: String,
        weight: Int,
        age: Int,
        athleticStat: Boolean

    ) {
        val user = UserEntity(
            name = name,
            city = city,
            birthDate = birthDate,
            job = job,
            email = email,
            password = password,
            weight = weight,
            age = age,
            athleticStat = athleticStat
        )

        viewModelScope.launch {
            userRepository.insertUser(user)
            Log.d(TAG, "User data inserted into the database: $user")
        }
    }

    fun getUserByEmail(email: String): LiveData<UserEntity?> {
        return userRepository.getUserByEmail(email)
    }

    fun getUserByEmailAndPassword(email: String, password: String): LiveData<UserEntity?> {
        Log.d(TAG, "getUserByEmailAndPassword: Start with Email: $email, Password: $password")

        // Assuming userRepository.getUserByEmailAndPassword returns a LiveData<UserEntity>
        val userLiveData = userRepository.getUserByEmailAndPassword(email, password)

        userLiveData.observeForever { userEntity ->
            Log.d(TAG, "getUserByEmailAndPassword: Observer triggered with UserEntity: $userEntity")
        }

        return userLiveData
    }

    fun updateUserProfile(user: UserEntity) {
        viewModelScope.launch {
            userRepository.updateUserProfile(user)
            Log.d(TAG, "User data updated into the database: $user")
        }
    }

    companion object {
        const val TAG = "UserViewModel"
    }
}