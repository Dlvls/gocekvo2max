package com.example.gocekvo2max.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Dao
interface BalkeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDataBalke(data: BalkeEntity)

    @Query("SELECT * FROM balke_table WHERE bTrackerId = :bTrackerId LIMIT 1")
    fun getBalkeDataById(bTrackerId: String): LiveData<BalkeEntity?>


    @Query("SELECT * FROM balke_table WHERE userId = :userId LIMIT 1")
    fun getBalkeDataByUserId(userId: String): LiveData<BalkeEntity?>

    @Query("SELECT oxygenCon FROM balke_table WHERE userId = :userId")
    suspend fun getAllOxygenCon(userId: String): List<String?>

    @Query("SELECT * FROM balke_table WHERE userId = :userId")
    suspend fun getAllBalkeData(userId: String): List<BalkeEntity>

    @Update
    suspend fun updateDataBalke(data: BalkeEntity)

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLatLngList(value: String?): List<LatLng>? {
        return Gson().fromJson(value, object : TypeToken<List<LatLng>>() {}.type)
    }

    @Delete
    suspend fun deleteDataBalkeById(balkeEntity: BalkeEntity)
}