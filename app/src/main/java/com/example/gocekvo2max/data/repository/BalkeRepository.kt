package com.example.gocekvo2max.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.gocekvo2max.data.database.RoomDB
import com.example.gocekvo2max.data.database.dao.BalkeDao
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BalkeRepository(application: Application) {
    private val mBalkeDao: BalkeDao

    init {
        val db = RoomDB.getDatabase(application)
        mBalkeDao = db.balkeDao()
    }

    suspend fun insertDataBalke(data: BalkeEntity) {
        withContext(Dispatchers.IO) {
            mBalkeDao.insertDataBalke(data)
        }
    }

    fun getBalkeDataById(bTrackerId: String): LiveData<BalkeEntity?> =
        mBalkeDao.getBalkeDataById(bTrackerId)

    fun getBalkeDataByUserId(userId: String): LiveData<BalkeEntity?> =
        mBalkeDao.getBalkeDataByUserId(userId)

    suspend fun updateDataBalke(data: BalkeEntity) {
        withContext(Dispatchers.IO) {
            mBalkeDao.updateDataBalke(data)
        }
    }

    suspend fun getAllOxygenCon(userId: String): List<String?> {
        return mBalkeDao.getAllOxygenCon(userId)
    }

    suspend fun getAllBalkeData(userId: String): List<BalkeEntity> {
        return mBalkeDao.getAllBalkeData(userId)
    }

    suspend fun deleteBalkeDataById(balkeEntity: BalkeEntity) {
        withContext(Dispatchers.IO) {
            mBalkeDao.deleteDataBalkeById(balkeEntity)
        }
    }
}