package com.example.gocekvo2max.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gocekvo2max.helper.BalkeTypeConverters
import com.google.android.gms.maps.model.LatLng
import java.util.Date

@Entity(tableName = "balke_table")
@TypeConverters(BalkeTypeConverters::class)
data class BalkeEntity(
    @PrimaryKey(autoGenerate = false)
    val bTrackerId: String,
    val userId: Int,
    val balkeDuration: Int? = null,
    val balkeDistance: Double? = null,
    val heartBeats: Int? = null,
    val heartLines: String? = null,
    val oxygenCon: String? = null,
    val polyLineData: List<LatLng>? = null,
    val currentDate: Date? = null
)