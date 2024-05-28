package com.example.gocekvo2max.oxygenlevel

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.MainActivity
import com.example.gocekvo2max.helper.OxygenLevelEvaluator
import com.example.gocekvo2max.R
import com.example.gocekvo2max.balke.BalkeTestActivity
import com.example.gocekvo2max.data.viewmodel.BalkeViewModel
import com.example.gocekvo2max.data.viewmodel.RockPortViewModel
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityOxygenLevelBinding
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale

class OxygenLevelActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityOxygenLevelBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var rockPortViewModel: RockPortViewModel
    private lateinit var balkeViewModel: BalkeViewModel
    private lateinit var mMap: GoogleMap
    private var polylineData: List<LatLng>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOxygenLevelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        rockPortViewModel = ViewModelProvider(this)[RockPortViewModel::class.java]
        balkeViewModel = ViewModelProvider(this)[BalkeViewModel::class.java]

        updateData()
        retrieveData()

        binding.btnOut.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        retrievePolylineData()
    }

    private fun updateData() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val source = intent.getStringExtra("source")
        Log.d(TAG, "Source: $source")

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->

                val weight = user?.weight?.toDouble()
                val gender = user?.gender.toGenderNumeric()
                val age = user?.age

                if (source == "BalkeActivity") {
                    val sharedPreferences =
                        getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
                    val balkeId = sharedPreferences.getString("id", null)

                    balkeViewModel.getBalkeDataById(balkeId.toString()).observeOnce { data ->
                        val heartRate = data?.heartBeats?.toDouble()
                        val duration = data?.balkeDuration?.toDouble()
                        val distance = data?.balkeDistance

                        val durationInMinutes = duration?.div(60000) ?: 0.0

                        val calculatedOxygenCon = if (distance != null) {
                            calculateBalkeVo2max(distance, durationInMinutes).toString()
                        } else {
                            // Handle the case when distance is null
                            "null"
                        }

                        if (calculatedOxygenCon != "null") { // Check if the calculated value is not null
                            val updateData = data?.copy(
                                oxygenCon = calculatedOxygenCon
                            )

                            updateData?.let {
                                balkeViewModel.updateDataBalke(it)
                                Log.d(TAG, "Balke Data Updated: $it")
                            }
                        }
                    }
                } else {
                    val sharedPreferences =
                        getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
                    val balkeId = sharedPreferences.getString("id", null)

                    rockPortViewModel.getRockPortDataById(balkeId.toString()).observeOnce { data ->
                        val heartRate = data?.heartBeats?.toDouble()
                        val duration = data?.rockportDuration?.toDouble()
                        val durationInMinutes = duration?.div(60000) ?: 0.0
                        val distanceInMeters =
                            data?.rockportDistance?.times(1000) ?: 0.0  // Convert km to meters

                        Log.d(TAG, "Weight: $weight")
                        Log.d(TAG, "Gender: $gender")
                        Log.d(TAG, "Age: $age")
                        Log.d(TAG, "Duration in Minutes: $durationInMinutes")
                        Log.d(TAG, "Distance in Meter: $distanceInMeters")
                        Log.d(TAG, "Heart Rate: $heartRate beats per minute")

                        val calculatedOxygenCon = calculateRockPortVo2max(
                            weight!!,
                            gender,
                            age!!,
                            durationInMinutes!!,
                            distanceInMeters!!,
                            heartRate!!
                        ).toString()

                        if (calculatedOxygenCon != "null") { // Check if the calculated value is not null
                            val updateData = data?.copy(
                                oxygenCon = calculatedOxygenCon
                            )

                            updateData?.let {
                                rockPortViewModel.updateDataRockPort(it)
                                Log.d(TAG, "Rock Port Data Updated: $it")
                            }
                        }
                    }
                }
            }
    }

    private fun <T> LiveData<T>.observeOnce(observer: (T) -> Unit) {
        observeForever(object : Observer<T> {
            override fun onChanged(value: T) {
                observer(value)
                removeObserver(this)
            }
        })
    }

//    private fun calculateVo2max(
//        weight: Double,
//        gender: Int,
//        time: Double,
//        heartRate: Double
//    ): Double {
//        return if (gender == 1) {
//            (108.844 - (0.1636 * weight) - (1.438 * time) - (0.1928 * heartRate))
//        } else {
//            (100.5 - (0.1636 * weight) - (1.438 * time) - (0.1928 * heartRate))
//        }
//
//    }

    private fun calculateRockPortVo2max(
        weight: Double,
        gender: Int,
        age: Int,
        time: Double,
        distance: Double,
        heartRate: Double
    ): Double {
        val weightInPounds = weight * 2.20462

        return (132.853 - (0.0769 * weightInPounds) - (0.3877 * age) + (6.315 * gender) - (3.2649 * time) - (0.1565 * heartRate) - (132 - (0.0825 * distance)))
    }

    private fun calculateBalkeVo2max(distance: Double, time: Double): Double {
        val distanceInMeters = distance * 1000.0 // Convert kilometers to meters
        return (((distanceInMeters / time) - 133) * 0.172 + 33.3)
    }

    private fun String?.toGenderNumeric(): Int {
        return when (this?.toLowerCase()) {
            "female" -> 0
            "male" -> 1
            else -> -1 // Handle other cases or return a default value as needed
        }
    }

    private fun retrieveData() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val source = intent.getStringExtra("source")
        Log.d(TAG, "Source: $source")

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->

                val age = user?.age
                val gender = user?.gender.toGenderNumeric()

                if (source == "BalkeActivity") {
                    val sharedPreferences =
                        getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
                    val balkeId = sharedPreferences.getString("id", null)
                    Log.d(TAG, "bId: $balkeId")

                    balkeViewModel.getBalkeDataById(balkeId.toString()).observe(this) { data ->
                        Log.d(TAG, "BalkeData: $data")
                        binding.tvOxygen.text =
                            data?.oxygenCon?.toDoubleOrNull()?.formatToOneDecimal() ?: ""
                        val vo2max = data?.oxygenCon?.toDouble()
                        Log.d(TAG, "Vo2max: $vo2max")

                        if (vo2max != null) {
                            val oxygenStatus = if (gender == 1) {
                                OxygenLevelEvaluator.evaluateStatusFemale(age ?: 0, vo2max)
                            } else {
                                OxygenLevelEvaluator.evaluateStatusMale(age ?: 0, vo2max)
                            }

                            binding.tvStatus.text = oxygenStatus

                            // Set text color based on status (if needed)
                            when (oxygenStatus) {
                                "Superior" -> binding.tvStatus.setTextColor(Color.parseColor("#008000")) // Green
                                "Excellent" -> binding.tvStatus.setTextColor(Color.parseColor("#008000")) // Green
                                "Good" -> binding.tvStatus.setTextColor(Color.parseColor("#0000FF")) // Blue
                                "Fair" -> binding.tvStatus.setTextColor(Color.parseColor("#FFFF00")) // Yellow
                                "Poor" -> binding.tvStatus.setTextColor(Color.parseColor("#FF0000")) // Red
                                else -> binding.tvStatus.setTextColor(Color.parseColor("#111111")) // Black (default)
                            }
                        }
                    }

                } else {
                    val sharedPreferences =
                        getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
                    val rpId = sharedPreferences.getString("id", null)
                    Log.d(TAG, "rpId: $rpId")

                    rockPortViewModel.getRockPortDataById(rpId.toString()).observe(this) { data ->
                        Log.d(TAG, "OxyCon: $data")
                        binding.tvOxygen.text =
                            data?.oxygenCon?.toDoubleOrNull()?.formatToOneDecimal() ?: ""
                        val vo2max = data?.oxygenCon?.toDouble()
                        Log.d(TAG, "Vo2max Rock Port: $vo2max")

                        if (vo2max != null) {
                            val oxygenStatus = if (gender == 1) {
                                OxygenLevelEvaluator.evaluateStatusFemale(age ?: 0, vo2max)
                            } else {
                                OxygenLevelEvaluator.evaluateStatusMale(age ?: 0, vo2max)
                            }

                            binding.tvStatus.text = oxygenStatus

                            // Set text color based on status (if needed)
                            when (oxygenStatus) {
                                "Superior" -> binding.tvStatus.setTextColor(Color.parseColor("#83D630")) // Green
                                "Excellent" -> binding.tvStatus.setTextColor(Color.parseColor("#1A659E")) // Green
                                "Good" -> binding.tvStatus.setTextColor(Color.parseColor("#83D630")) // Blue
                                "Fair" -> binding.tvStatus.setTextColor(Color.parseColor("#FFBE0B")) // Yellow
                                "Poor" -> binding.tvStatus.setTextColor(Color.parseColor("#FF0000")) // Red
                                else -> binding.tvStatus.setTextColor(Color.parseColor("#111111")) // Black (default)
                            }
                        }

                    }
                }
            }
    }

    private fun Double.formatToOneDecimal(): String {
        return String.format(Locale.US, "%.1f", this)
    }

    private fun retrievePolylineData() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val source = intent.getStringExtra("source")
        Log.d(TAG, "Source: $source")

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->

                if (source == "BalkeActivity") {
                    val sharedPreferences =
                        getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
                    val balkeId = sharedPreferences.getString("id", null)

                    balkeViewModel.getBalkeDataById(balkeId.toString()).observe(this) { data ->
                        polylineData = data?.polyLineData
                        drawPolylineOnMap()
                    }
                } else {
                    val sharedPreferences =
                        getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
                    val rpId = sharedPreferences.getString("id", null)

                    rockPortViewModel.getRockPortDataById(rpId.toString()).observe(this) { data ->
                        polylineData = data?.polyLineData
                        drawPolylineOnMap()
                    }
                }
            }
    }

    private fun drawPolylineOnMap() {
        polylineData?.let { data ->
            if (data.isNotEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(data)
                    .color(Color.CYAN)
                    .width(10f)
                mMap.addPolyline(polylineOptions)

                val boundsBuilder = LatLngBounds.Builder()
                for (point in data) {
                    boundsBuilder.include(point)
                }
                val cameraUpdate = if (isLandscape()) {
                    Log.d(TAG, "This is cameraLandscapeUpdate condition")
                    getLandscapeCameraUpdate(boundsBuilder.build())
                } else {
                    Log.d(TAG, "This is cameraUpdate condition")
                    getCameraUpdate(boundsBuilder.build())
                }
                mMap.moveCamera(cameraUpdate)
            }
        }
    }

    private fun isLandscape(): Boolean {
        Log.d(TAG,"It's landscape")
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun getCameraUpdate(latLngBounds: LatLngBounds): CameraUpdate {
        Log.d(TAG, "This is getCameraUpdate")
        val mapWidth = binding.root.width
        val mapHeight = binding.root.height

        // Check if the view size is too small
        if (mapWidth <= 0 || mapHeight <= 0) {
            Log.e(TAG, "View size is too small: Width=$mapWidth, Height=$mapHeight")
            return CameraUpdateFactory.newLatLng(latLngBounds.center)
        }

        // Calculate padding
        val paddingPercentage = 0.2 // Adjust the percentage as needed
        val horizontalPadding = (mapWidth * paddingPercentage).toInt()
        val verticalPadding = (mapHeight * paddingPercentage).toInt()

        // Set a minimum padding value
        val minPadding = 100
        val padding = Integer.max(minPadding, Integer.max(horizontalPadding, verticalPadding))

        // Create CameraUpdate
        return CameraUpdateFactory.newLatLngBounds(latLngBounds, padding)
    }

    private fun getLandscapeCameraUpdate(latLngBounds: LatLngBounds): CameraUpdate {
        val mapWidth = binding.root.width
        val mapHeight = binding.root.height

        // Check if the view size is too small
        if (mapWidth <= 0 || mapHeight <= 0) {
            Log.e(TAG, "View size is too small: Width=$mapWidth, Height=$mapHeight")
            return CameraUpdateFactory.newLatLng(latLngBounds.center)
        }
        val finalPadding = 0
        return CameraUpdateFactory.newLatLngBounds(latLngBounds, finalPadding)
    }


    private fun oxygenLevelStatus(vo2max: Double): String {
        return when {
            vo2max > 49.0 -> {
                binding.tvStatus.setTextColor(Color.parseColor("#008000")) // Green
                "Superior"
            }

            vo2max >= 44.0 -> {
                binding.tvStatus.setTextColor(Color.parseColor("#008000")) // Green
                "Excellent"
            }

            vo2max >= 39.0 -> {
                binding.tvStatus.setTextColor(Color.parseColor("#0000FF")) // Blue
                "Good"
            }

            vo2max >= 34.0 -> {
                binding.tvStatus.setTextColor(Color.parseColor("#FFFF00")) // Yellow
                "Fair"
            }

            vo2max >= 25.0 -> {
                binding.tvStatus.setTextColor(Color.parseColor("#FF0000")) // Red
                "Poor"
            }

            else -> {
                binding.tvStatus.setTextColor(Color.parseColor("#000000")) // Black (default)
                "Unknown"
            }
        }
    }

    companion object {
        const val TAG = "OxygenLevelActivity"
        private const val cameraPadding = 300
    }

}

