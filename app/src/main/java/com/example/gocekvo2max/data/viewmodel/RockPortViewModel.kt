package com.example.gocekvo2max.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gocekvo2max.data.database.entity.RockPortEntity
import com.example.gocekvo2max.data.repository.RockPortRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.util.Date

class RockPortViewModel(application: Application) : AndroidViewModel(application) {

    private val rockPortRepository = RockPortRepository(application)

    fun insertDataRockPort(
        rpTrackerId: String,
        userId: Int,
        rockportDuration: Int,
        rockportDistance: Double,
        polyLineData: List<LatLng>,
        currentDate: Date

    ) {
        val data = RockPortEntity(
            rpTrackerId = rpTrackerId,
            userId = userId,
            rockportDuration = rockportDuration,
            rockportDistance = rockportDistance,
            polyLineData = polyLineData,
            currentDate = currentDate
        )

        viewModelScope.launch {
            rockPortRepository.insertDataRockPort(data)
            Log.d(TAG, "Data inserted into the database: $data")
        }
    }

    fun getRockPortDataById(rpTrackerId: String): LiveData<RockPortEntity?> {
        return rockPortRepository.getRockPortDataById(rpTrackerId)
    }

    private val _oxygenConList = MutableLiveData<List<String?>>()
    val oxygenConList: LiveData<List<String?>>
        get() = _oxygenConList

    fun getAllOxygenCon(userId: String) {
        viewModelScope.launch {
            _oxygenConList.value = rockPortRepository.getAllOxygenCon(userId)
        }
    }

    fun updateDataRockPort(data: RockPortEntity) {
        viewModelScope.launch {
            rockPortRepository.updateDataRockPort(data)
            Log.d(TAG, "Data updated into the database: $data")
        }
    }

    private val _rockPortList = MutableLiveData<List<RockPortEntity>>()
    val rockPortList: LiveData<List<RockPortEntity>>
        get() = _rockPortList

    fun getAllRockPortData(userId: String) {
        viewModelScope.launch {
            _rockPortList.value = rockPortRepository.getAllRockPortData(userId)
        }
    }

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean>
        get() = _deleteResult

    fun deleteRockPortData(rockPortEntity: RockPortEntity) {
        viewModelScope.launch {
            try {
                rockPortRepository.deleteRockPortData(rockPortEntity)
                _deleteResult.postValue(true)
            } catch (e: Exception) {
                // Handle exceptions as needed
                _deleteResult.postValue(false)
            }
        }
    }

    companion object {
        const val TAG = "TrackerViewModel"
    }
}