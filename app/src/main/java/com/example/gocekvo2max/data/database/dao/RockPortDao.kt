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
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Dao
interface RockPortDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDataRockPort(data: RockPortEntity)

    @Query("SELECT * FROM rock_port_table WHERE rpTrackerId = :rpTrackerId LIMIT 1")
    fun getRockPortDataById(rpTrackerId: String): LiveData<RockPortEntity?>

    @Query("SELECT oxygenCon FROM rock_port_table WHERE userId = :userId")
    suspend fun getAllOxygenCon(userId: String): List<String?>

    @Update
    suspend fun updateDataRockPort(data: RockPortEntity)

    @Query("SELECT * FROM rock_port_table WHERE userId = :userId")
    suspend fun getAllRockPortData(userId: String): List<RockPortEntity>

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>?): String? {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toLatLngList(value: String?): List<LatLng>? {
        return Gson().fromJson(value, object : TypeToken<List<LatLng>>() {}.type)
    }

    @Delete
    suspend fun deleteDataRockPort(rockPortEntity: RockPortEntity)
}