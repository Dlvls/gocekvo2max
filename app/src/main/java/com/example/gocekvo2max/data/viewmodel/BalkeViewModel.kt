package com.example.gocekvo2max.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gocekvo2max.data.database.entity.BalkeEntity
import com.example.gocekvo2max.data.repository.BalkeRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.util.Date

class BalkeViewModel(application: Application) : AndroidViewModel(application) {

    private val balkeRepository = BalkeRepository(application)

    fun insertDataBalke(
        bTrackerId: String,
        userId: Int,
        balkeDuration: Int,
        balkeDistance: Double,
        polyLineData: List<LatLng>,
        currentDate: Date
    ) {
        val data = BalkeEntity(
            bTrackerId = bTrackerId,
            userId = userId,
            balkeDuration = balkeDuration,
            balkeDistance = balkeDistance,
            polyLineData = polyLineData,
            currentDate = currentDate
        )

        viewModelScope.launch {
            balkeRepository.insertDataBalke(data)
            Log.d(TAG, "Data inserted into the database: $data")
        }
    }

    fun getBalkeDataById(bTrackerId: String): LiveData<BalkeEntity?> {
        return balkeRepository.getBalkeDataById(bTrackerId)
    }

    private val _oxygenConList = MutableLiveData<List<String?>>()
    val oxygenConList: LiveData<List<String?>>
        get() = _oxygenConList

    fun getAllOxygenCon(userId: String) {
        viewModelScope.launch {
            _oxygenConList.value = balkeRepository.getAllOxygenCon(userId)
        }
    }

    private val _balkeList = MutableLiveData<List<BalkeEntity>>()
    val balkeList: LiveData<List<BalkeEntity>>
        get() = _balkeList

    fun getAllBalkeData(userId: String) {
        viewModelScope.launch {
            _balkeList.value = balkeRepository.getAllBalkeData(userId)
        }
    }

    fun updateDataBalke(data: BalkeEntity) {
        viewModelScope.launch {
            balkeRepository.updateDataBalke(data)
            Log.d(TAG, "Data updated into the database: $data")
        }
    }

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean>
        get() = _deleteResult

    fun deleteBalkeDataById(balkeEntity: BalkeEntity) {
        viewModelScope.launch {
            try {
                balkeRepository.deleteBalkeDataById(balkeEntity)
                _deleteResult.postValue(true)
            } catch (e: Exception) {
                _deleteResult.postValue(false)
            }
        }
    }

    companion object {
        const val TAG = "BalkeViewModel"
    }
}