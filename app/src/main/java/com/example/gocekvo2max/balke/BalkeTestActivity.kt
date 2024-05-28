package com.example.gocekvo2max.balke

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.R
import com.example.gocekvo2max.auth.LoginActivity
import com.example.gocekvo2max.data.viewmodel.BalkeViewModel
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityBalkeTestBinding
import com.example.gocekvo2max.databinding.DoneBinding
import com.example.gocekvo2max.oxygenlevel.OxygenLevelActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.lang.Integer.max
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class BalkeTestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityBalkeTestBinding
    private lateinit var viewModel: BalkeViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest
    private var isTracking = false
    private lateinit var locationCallback: LocationCallback
    private lateinit var chronometer: Chronometer

    private var allLatLng = ArrayList<LatLng>()

    private var totalDistanceMeters: Double = 0.0
    private var lastChronometerTime: Long = 0

    private var isPaused = false
    private var lastLocation: Location? = null

    private var boundsBuilder = LatLngBounds.Builder()

    private var handler: Handler? = null
    private var stopTrackingRunnable: Runnable? = null

    private lateinit var mediaPlayer: MediaPlayer

    private var isMapReady = false

    private var timeTrackingHandler: Handler? = null
    private var timeTrackingRunnable: Runnable? = null
    private val timeTrackingIntervalMillis = 1000L

    private var btnStartState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBalkeTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[BalkeViewModel::class.java]

        userViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[UserViewModel::class.java]

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        chronometer = binding.chronometer

        mediaPlayer = MediaPlayer.create(this, R.raw.times_up)
        mediaPlayer.setVolume(1.0f, 1.0f)
        mediaPlayer.isLooping = true

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (savedInstanceState != null) {
            totalDistanceMeters = savedInstanceState.getDouble("totalDistance", 0.0)
            lastChronometerTime = savedInstanceState.getLong("lastChronometerTime", 0)
            isTracking = savedInstanceState.getBoolean("isTracking", false)
            isPaused = savedInstanceState.getBoolean("isPaused", false)
            btnStartState = savedInstanceState.getBoolean("btnStartState", false)
            allLatLng = savedInstanceState.getParcelableArrayList("allLatLng") ?: ArrayList()

            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.release()
                mediaPlayer.prepareAsync()
            } else {
//                mediaPlayer.start()
            }

            savedInstances()

            if (btnStartState) {
                if (handler != null && stopTrackingRunnable != null) {
                    // The activity is being recreated, don't create new instances
                    Log.d(TAG, "Handler and Runnable are not null, not creating new instances.")
                } else {
                    chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime
                    chronometer.start()

                    binding.btnStart.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE

                    val locationList = convertLatLngListToLocationList(allLatLng)
                    Log.d(TAG, "Location List: $locationList")
                    locationList.isNotEmpty()
                    updateDistanceIfTracking(locationList[0])

                    Log.d(TAG, "lastChronometerTime = $lastChronometerTime")

                    val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(lastChronometerTime)

                    Log.d(TAG, "elapsedMinutes = $elapsedMinutes")

                    if (elapsedMinutes < 15) {
                        Log.d(TAG, "Keep Tracking")
                    } else {
                        insertData()
//                        showFinishedDialog()
//                        mediaPlayer.release()
                    }

                    insertData()
                }
            } else {
                updateTrackingStatus(false)
                stopLocationUpdates()
                // Set the chronometer base time to the last known time
                binding.btnStart.visibility = View.VISIBLE
                binding.btnStop.visibility = View.GONE

                chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime
            }
        }
    }

    private fun convertLatLngListToLocationList(latLngList: List<LatLng>): List<Location> {
        val locationList = mutableListOf<Location>()
        for (latLng in latLngList) {
            val location = Location("")
            location.latitude = latLng.latitude
            location.longitude = latLng.longitude
            locationList.add(location)
        }
        return locationList
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDouble("totalDistance", totalDistanceMeters)
        outState.putLong("lastChronometerTime", lastChronometerTime)
        outState.putBoolean("isTracking", isTracking)
        outState.putBoolean("isPaused", isPaused)
        outState.putBoolean("btnStartState", btnStartState)
        outState.putParcelableArrayList("allLatLng", allLatLng)
        outState.putBoolean("startButtonVisible", binding.btnStart.visibility == View.VISIBLE)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        createLocationRequest()
        createLocationCallback()

        binding.btnStart.setOnClickListener {
            // Clear the map and reset the distance before starting tracking
            clearMaps()

            btnStartState = true
            Log.d(TAG, "btnStart pressed: $btnStartState")

            totalDistanceMeters = 0.0
            updateDistanceTextView(totalDistanceMeters)

            // Start tracking
            mMap.isMyLocationEnabled = true
            updateTrackingStatus(true)
            startLocationUpdates()
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()

            startTimeTracking()

            stopTrackingAfterDuration(15 * 60 * 1000)
//            stopTrackingAfterDuration(2 * 60 * 1000)
        }

        binding.btnStop.setOnClickListener {
            updateTrackingStatus(false)
            stopLocationUpdates()
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
            chronometer.stop()

            // Capture the current chronometer time
            lastChronometerTime = SystemClock.elapsedRealtime() - chronometer.base
            Log.d(TAG, "This is btnStop lastChronometerTime: $lastChronometerTime")

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = false
            }

            chronometer.stop()
            chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime

            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(lastChronometerTime)

            if (elapsedMinutes < 15) {
                showNotFinishedDialog()
                Log.d(TAG, "This is elapsedMinutes after landscape")
                mediaPlayer.start()
            } else {
                insertData()
                showFinishedDialog()
                mediaPlayer.release()
            }
        }
        isMapReady = true
    }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.uiSettings.isZoomControlsEnabled = true
            mMap.uiSettings.isIndoorLevelPickerEnabled = true
            mMap.uiSettings.isCompassEnabled = true
            mMap.uiSettings.isMapToolbarEnabled = true

            mMap.isMyLocationEnabled = true
            // Request the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLocation = LatLng(it.latitude, it.longitude)
                        val zoomLevel = 15f // Adjust this value as needed
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation,
                                zoomLevel
                            )
                        )
                        Log.d("MapsActivity", "Location: $currentLocation")
                    } ?: run {
                        Log.d("MapsActivity", "Last known location is null")
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private val resolutionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                RESULT_OK ->
                    Log.i(TAG, "onActivityResult: All location settings are satisfied.")

                RESULT_CANCELED ->
                    Toast.makeText(
                        this@BalkeTestActivity,
                        getString(R.string.you_need_to_enable_gps_to_use_this_application),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }

    private fun createLocationRequest() {
        Log.d(TAG, "This is createLocationRequest after rotate")
        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(1)
            maxWaitTime = TimeUnit.SECONDS.toMillis(1)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                getMyLocation()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        resolutionLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Toast.makeText(this@BalkeTestActivity, sendEx.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
    }

    private fun updateTrackingStatus(newStatus: Boolean) {
        Log.d(TAG, "This is updateTrackingStatus")
        isTracking = newStatus
        if (isTracking) {
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        } else {
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
        }
    }

    private fun createLocationCallback() {
        Log.d(TAG, "This is createLocationCallback after rotate")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d(TAG, "onLocationResult: " + location.latitude + ", " + location.longitude)
                    updateDistanceIfTracking(location)
                }
            }
        }
    }

    private fun updateDistanceIfTracking(location: Location) {
        Log.d(TAG, "This is updateDistanceIfTracking")
        if (isTracking && isMapReady) {
            Log.d(TAG, "i'm going further")
            if (!isPaused) {
                Log.d(TAG, "further?")
                val lastLatLng = LatLng(location.latitude, location.longitude)

                allLatLng.add(lastLatLng)
                mMap.addPolyline(
                    PolylineOptions()
                        .color(Color.CYAN)
                        .width(10f)
                        .addAll(allLatLng)
                )

                if (allLatLng.size > 1) {
                    Log.d(TAG, "way further?")
                    val distance = calculateDistance(
                        allLatLng[allLatLng.size - 2],
                        allLatLng[allLatLng.size - 1]
                    )
                    totalDistanceMeters += distance

                    Log.d(TAG, "Updated distance: $totalDistanceMeters meters")

                    updateDistanceTextView(totalDistanceMeters)

                    boundsBuilder.include(lastLatLng)
                    Log.d(TAG, "make it?")

                    val cameraUpdate = if (isLandscape()) {
                        Log.d(TAG, "am i?")
                        getLandscapeCameraUpdate(boundsBuilder.build())
                    } else {
                        getCameraUpdate(boundsBuilder.build())
                    }

                    mMap.moveCamera(cameraUpdate)
                }
            } else {
                val lastLatLng = LatLng(location.latitude, location.longitude)
//            else if (lastLocation != null) {
//                val lastLatLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)

                allLatLng.add(lastLatLng)
                mMap.addPolyline(
                    PolylineOptions()
                        .color(Color.CYAN)
                        .width(10f)
                        .addAll(allLatLng)
                )

                if (allLatLng.size > 1) {
                    val distance = calculateDistance(
                        allLatLng[allLatLng.size - 2],
                        allLatLng[allLatLng.size - 1]
                    )
                    totalDistanceMeters += distance
                    updateDistanceTextView(totalDistanceMeters)

                    val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(lastChronometerTime)

                    if (elapsedMinutes < 15) {
                        showNotFinishedDialog()
                        Log.d(TAG, "This is elapsedMinutes after landscape")
                        mediaPlayer.start()
                    } else {
                        insertData()
                        showFinishedDialog()
                        mediaPlayer.release()
                    }

                    boundsBuilder.include(lastLatLng)

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
    }

    private fun isLandscape(): Boolean {
        Log.d(TAG, "It's landscape")
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
        val padding = max(minPadding, max(horizontalPadding, verticalPadding))

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

        // Set padding to zero
        val finalPadding = 0

        // Create CameraUpdate
        return CameraUpdateFactory.newLatLngBounds(latLngBounds, finalPadding)
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (exception: SecurityException) {
            Log.e(TAG, "Error : " + exception.message)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun calculateDistance(latlng1: LatLng, latlng2: LatLng): Double {
        val R = 6371.0
        val lat1 = Math.toRadians(latlng1.latitude)
        val lon1 = Math.toRadians(latlng1.longitude)
        val lat2 = Math.toRadians(latlng2.latitude)
        val lon2 = Math.toRadians(latlng2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = Math.pow(Math.sin(dlat / 2), 2.0) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(
            Math.sin(dlon / 2), 2.0
        )
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c * 1000
    }

    private fun updateDistanceTextView(distanceMeters: Double) {
        val distanceKilometers = distanceMeters / 1000.0
        val formattedDistance = String.format(Locale.US, "%.2f", distanceKilometers)
        binding.tvDistance.text = formattedDistance
    }

    private fun stopTrackingAfterDuration(durationMillis: Long) {
        handler = Handler()
        stopTrackingRunnable = Runnable {
            // Code to stop tracking goes here
            updateTrackingStatus(false)
            stopLocationUpdates()
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
            chronometer.stop()

            lastChronometerTime = SystemClock.elapsedRealtime() - chronometer.base
            Log.d(TAG, "This is not the shared preference time: $lastChronometerTime")

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = false
            }

//            chronometer.stop()
            chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime

            insertData()

            showFinishedDialog()
        }
        handler?.postDelayed(stopTrackingRunnable!!, durationMillis)
    }

    private fun startTimeTracking() {
        timeTrackingHandler = Handler()
        timeTrackingRunnable = object : Runnable {
            override fun run() {
                if (isTracking) {
                    lastChronometerTime = SystemClock.elapsedRealtime() - chronometer.base
                    saveLastChronometerTime(lastChronometerTime)
                    Log.d(TAG, "Time is being tracked. Last Chronometer Time: $lastChronometerTime")
                } else {
                    Log.d(TAG, "Time is not being tracked.")
                }
                timeTrackingHandler?.postDelayed(this, timeTrackingIntervalMillis)
            }
        }
        timeTrackingHandler?.postDelayed(timeTrackingRunnable!!, timeTrackingIntervalMillis)
    }

    private fun stopTimeTracking() {
        timeTrackingHandler?.removeCallbacks(timeTrackingRunnable!!)
    }

    private fun saveLastChronometerTime(lastChronometerTime: Long) {
        val sharedPrefs = getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong("lastChronometerTime", lastChronometerTime)
        editor.apply()
    }

    private fun showFinishedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.done, null)
        val dialogBinding = DoneBinding.bind(dialogView)
        dialogBinding.imgDone.setImageResource(R.drawable.ic_done)
        dialogBinding.tvDone.text = getString(R.string.dialog_done)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setOnDismissListener {
                stopMediaPlayer()
            }
            .create()

        // Find and set an onClickListener for your custom "OK" button
        val doneButton = dialogBinding.btnDone
        doneButton.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(this, OxygenLevelActivity::class.java)
            intent.putExtra("source", "BalkeActivity")
            startActivity(intent)
        }

        dialog.show()
        mediaPlayer.start()
    }

    private fun showNotFinishedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.done, null)
        val dialogBinding = DoneBinding.bind(dialogView)
        dialogBinding.imgDone.setImageResource(R.drawable.ic_close)
        dialogBinding.tvDone.text = getString(R.string.dialog_not_finished_bk)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setOnDismissListener {
//                stopMediaPlayer()
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.prepareAsync()
                }
            }
            .create()

        // Find and set an onClickListener for your custom "OK" button
        val okButton = dialogBinding.btnDone
        okButton.setOnClickListener {
            dialog.dismiss()

            // Resume tracking
            updateTrackingStatus(true)
            startLocationUpdates()
            chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime
            chronometer.start()
        }

        dialog.show()
        mediaPlayer.start()
    }

    private fun stopMediaPlayer() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        } catch (e: IllegalStateException) {
            // Handle the exception as needed
            e.printStackTrace()
        }
    }

    private fun insertData() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val formattedDistance = binding.tvDistance.text.toString().replace(",", ".").toDouble()

        Log.d(TAG, "userEmail: $userEmail")
        Log.d(TAG, "userPassword: $userPassword")

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                Log.d(TAG, "This is inside InsertData function")
                // Insert data into the database
                val bTrackerId = UUID.randomUUID().toString()
                val userId = user?.userId
                val balkeDuration = lastChronometerTime.toInt()
                val balkeDistance = formattedDistance
                val polyLineData = allLatLng
                val currentDate = Date(System.currentTimeMillis())

                if (userId != null) {
                    Log.d(TAG, "User found. UserId: $userId")

                    viewModel.insertDataBalke(
                        bTrackerId,
                        userId,
                        balkeDuration,
                        balkeDistance,
                        polyLineData,
                        currentDate
                    )
                    Log.d(
                        TAG,
                        "Data inserted into ViewModel. bTrackerId: $bTrackerId, UserId: $userId, Duration: $balkeDuration, Distance: $balkeDistance, PolylineDataSize: ${polyLineData.size}, Date: $currentDate"
                    )
                    saveBalkeCredentials(bTrackerId)
                    Log.d(TAG, "bId: $bTrackerId")
                }
            }
    }

    private fun saveBalkeCredentials(id: String) {
        val sharedPrefs = getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putString("id", id)
        editor.apply()
        Log.d(TAG, "bId: $id")
    }

    private fun savedInstances() {
        updateDistanceTextView(totalDistanceMeters)

        createLocationCallback()
        createLocationRequest()

        updateTrackingStatus(true)
        startLocationUpdates()

        // Log the retrieved values
        Log.d(TAG, "Retrieved distance: $totalDistanceMeters meters")
        Log.d(TAG, "Retrieved lastChronometerTime: $lastChronometerTime")
        Log.d(TAG, "Retrieved isTracking: $isTracking")
        Log.d(TAG, "Retrieved isPaused: $isPaused")
    }

    override fun onResume() {
        super.onResume()
        if (isTracking) {
            if (::locationRequest.isInitialized) {
                startLocationUpdates()
            } else {
                Log.e(TAG, "LocationRequest has not been initialized.")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun clearMaps() {
        mMap.clear()
        allLatLng.clear()
        boundsBuilder = LatLngBounds.Builder()

        stopTimeTracking()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "BalkeTestActivity"
        private const val NOTIFICATION_CHANNEL_ID = "tracking_notification_channel"
    }
}