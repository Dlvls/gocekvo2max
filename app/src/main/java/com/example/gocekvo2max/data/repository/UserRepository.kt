package com.example.gocekvo2max.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.gocekvo2max.data.database.RoomDB
import com.example.gocekvo2max.data.database.dao.UserDao
import com.example.gocekvo2max.data.database.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(application: Application) {
    private val mUserDao: UserDao

    init {
        val db = RoomDB.getDatabase(application)
        mUserDao = db.userDao()
    }

    suspend fun insertUser(user: UserEntity) {
        withContext(Dispatchers.IO) {
            mUserDao.insertUser(user)
        }
    }

    suspend fun updateUserProfile(user: UserEntity) {
        withContext(Dispatchers.IO) {
            mUserDao.updateUserProfile(user)
        }
    }

    fun getUserByEmail(email: String): LiveData<UserEntity?> {
        return mUserDao.getUserByEmail(email)
    }

    fun getUserByEmailAndPassword(email: String, password: String): LiveData<UserEntity?> {
        return mUserDao.getUserByEmailAndPassword(email, password)
    }
}