package com.example.gocekvo2max.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val userId: Int? = null,
    val name: String,
    val city: String,
    val birthDate: Long,
    val job: String,
    val email: String? = null,
    val password: String? = null,
    val weight: Int? = 0,
    val age: Int? = 0,
    val athleticStat: Boolean,
    val gender: String? = null
)