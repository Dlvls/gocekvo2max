package com.example.gocekvo2max.helper

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class BalkeTypeConverters {
    @TypeConverter
    fun fromLatLngList(value: List<LatLng>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toLatLngList(value: String?): List<LatLng>? {
        return Gson().fromJson(value, object : TypeToken<List<LatLng>>() {}.type)
    }

    @TypeConverter
    fun fromDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun toDate(date: Date?): Long? {
        return date?.time
    }
}
