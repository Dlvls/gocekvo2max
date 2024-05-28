package com.example.gocekvo2max.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gocekvo2max.helper.BalkeTypeConverters
import com.example.gocekvo2max.data.database.dao.BalkeDao
import com.example.gocekvo2max.data.database.dao.RockPortDao
import com.example.gocekvo2max.data.database.dao.UserDao
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import com.example.gocekvo2max.data.database.entity.UserEntity

@Database(
    entities = [UserEntity::class, RockPortEntity::class, BalkeEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(BalkeTypeConverters::class)
abstract class RoomDB : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun rockPortDao(): RockPortDao
    abstract fun balkeDao(): BalkeDao

    companion object {
        @Volatile
        private var INSTANCE: RoomDB? = null

        @JvmStatic
        fun getDatabase(context: Context): RoomDB {
            if (INSTANCE == null) {
                synchronized(RoomDB::class.java) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext, RoomDB::class.java, "room_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE as RoomDB
        }
    }
}
