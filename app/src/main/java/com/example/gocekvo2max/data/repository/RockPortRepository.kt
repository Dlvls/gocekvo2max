package com.example.gocekvo2max.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.gocekvo2max.data.database.RoomDB
import com.example.gocekvo2max.data.database.dao.RockPortDao
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RockPortRepository(application: Application) {
    private val mRockPortDao: RockPortDao

    init {
        val db = RoomDB.getDatabase(application)
        mRockPortDao = db.rockPortDao()
    }

    suspend fun insertDataRockPort(data: RockPortEntity) {
        withContext(Dispatchers.IO) {
            mRockPortDao.insertDataRockPort(data)
        }
    }

    fun getRockPortDataById(rpTrackerId: String): LiveData<RockPortEntity?> =
        mRockPortDao.getRockPortDataById(rpTrackerId)

    suspend fun updateDataRockPort(data: RockPortEntity) {
        withContext(Dispatchers.IO) {
            mRockPortDao.updateDataRockPort(data)
        }
    }

    suspend fun getAllOxygenCon(userId: String): List<String?> {
        return mRockPortDao.getAllOxygenCon(userId)
    }

    suspend fun getAllRockPortData(userId: String): List<RockPortEntity> {
        return mRockPortDao.getAllRockPortData(userId)
    }

    suspend fun deleteRockPortData(rockPortEntity: RockPortEntity) {
        withContext(Dispatchers.IO) {
            mRockPortDao.deleteDataRockPort(rockPortEntity)
        }
    }
}