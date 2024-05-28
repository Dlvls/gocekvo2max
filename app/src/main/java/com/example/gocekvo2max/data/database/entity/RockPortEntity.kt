package com.example.gocekvo2max.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gocekvo2max.helper.BalkeTypeConverters
import com.google.android.gms.maps.model.LatLng
import java.util.Date

@Entity(tableName = "rock_port_table")
@TypeConverters(BalkeTypeConverters::class)
data class RockPortEntity(
    @PrimaryKey(autoGenerate = false)
    val rpTrackerId: String,
    val userId: Int,
    val rockportDuration: Int? = null,
    val rockportDistance: Double? = null,
    val heartBeats: Int? = null,
    val heartLines: String? = null,
    val oxygenCon: String? = null,
    val polyLineData: List<LatLng>? = null,
    val currentDate: Date? = null
)