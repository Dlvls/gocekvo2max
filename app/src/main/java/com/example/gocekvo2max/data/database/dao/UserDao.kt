package com.example.gocekvo2max.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gocekvo2max.data.database.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM user_table WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: Int): LiveData<UserEntity?>

    @Query("SELECT * FROM user_table ORDER BY userId ASC")
    fun getAllUser(): LiveData<List<UserEntity>>

    @Query("SELECT * FROM user_table LIMIT 1")
    fun getUser(): LiveData<UserEntity?>

    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): LiveData<UserEntity?>

    @Query("SELECT * FROM user_table WHERE email = :email AND password = :password LIMIT 1")
    fun getUserByEmailAndPassword(email: String, password: String): LiveData<UserEntity?>

    @Update
    suspend fun updateUserProfile(user: UserEntity)
}