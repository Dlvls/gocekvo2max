package com.example.gocekvo2max.maps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.R
import com.example.gocekvo2max.data.viewmodel.BalkeViewModel
import com.example.gocekvo2max.data.viewmodel.UserViewModel
import com.example.gocekvo2max.databinding.ActivityBalkeTestBinding
import com.example.gocekvo2max.oxygenlevel.OxygenLevelActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinalBalkeTestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityBalkeTestBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var timeTrackingHandler: Handler
    private lateinit var timeTrackingRunnable: Runnable
    private lateinit var userViewModel: UserViewModel
    private lateinit var viewModel: BalkeViewModel

    private var mapReady: Boolean = false
    private var startTimeMillis: Long = 0
    private var isTrackingStarted: Boolean = false
    private var lastChronometerTime: Long = 0
    private var mediaPlayer: MediaPlayer? = null
    private var totalDistanceMeters: Double = 0.0
    private var allLatLng = ArrayList<LatLng>()
    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var isTrackingRestored: Boolean = false
    private var accumulatedDistance: Double = 0.0

    private val timeTrackingIntervalMillis = 1000L
    private val handler = Handler()
    private val stopTrackingRunnable = Runnable { stopTracking() }
    private val polylinePoints = mutableListOf<LatLng>()

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
        mediaPlayer = MediaPlayer.create(this, R.raw.times_up)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkGPSStatus()
            showCurrentLocation()
        }

        binding.btnStart.setOnClickListener {
            toggleTracking()
        }

        savedInstanceState?.let { savedState ->
            lastChronometerTime = savedState.getLong("lastChronometerTime", 0)
            isTrackingRestored = savedState.getBoolean("isTrackingStarted", false)
            allLatLng = savedState.getParcelableArrayList("allLatLng") ?: ArrayList()

            Log.d(TAG, "Retrieved lastChronometerTime: $lastChronometerTime")
            Log.d(TAG, "Retrieved isTrackingRestored: $isTrackingRestored")
            Log.d(TAG, "Retrieved allLatLng: $allLatLng")

            if (isTrackingRestored) {
                Log.d(TAG, "Resuming tracking...")
                startTracking()

                Log.d(TAG, "Retrieved totalDistanceMeters: $totalDistanceMeters")
                totalDistanceMeters = savedState.getDouble("totalDistanceMeters", 0.0)

                if (lastChronometerTime != 0L) {
                    binding.chronometer.base = SystemClock.elapsedRealtime() - lastChronometerTime
                    binding.chronometer.start()
                }

                binding.tvDistance.text = String.format(Locale.US, "%.2f", totalDistanceMeters)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = true
        mapReady = true
        showCurrentLocation()
        startTrackingAlertDialog()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGPSStatus()
            }
        }
    }

//    private fun showCurrentLocation() {
//        if (mapReady) {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                mMap.isMyLocationEnabled = true
//                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//                    if (location != null) {
//                        val currentLatLng = LatLng(location.latitude, location.longitude)
//                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
//                    }
//                }
//            } else {
//                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun showCurrentLocation() {
        if (mapReady) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    }
                }
            } else {
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkGPSStatus() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setMessage(Html.fromHtml("<font color='#000000'>GPS is not enabled or does not provide high accuracy. Please turn on high accuracy GPS to use this app.</font>"))
                .setPositiveButton("Yes") { _, _ ->
                    startActivityForResult(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        REQUEST_ENABLE_GPS
                    )
                }
                .setNegativeButton("No") { _, _ ->
                    showCurrentLocation()
                }
                .show()
        } else {
            showCurrentLocation()
        }
    }

    private fun toggleTracking() {
        if (isTrackingStarted) {
            handler.removeCallbacks(stopTrackingRunnable)
            stopTracking()
        } else {
            startTracking()
        }
    }

    private fun startTrackingAlertDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(Html.fromHtml("<font color='#000000'>Wait for Calibration</font>"))
            .setMessage(Html.fromHtml("<font color='#000000'>Please wait for a moment to calibrate your location, then press start.</font>"))
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        alertDialog.show()
    }


    private fun startTracking() {
        startTimeMillis = System.currentTimeMillis()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult ?: return
                    for (location in locationResult.locations) {
                        if (location != null) {
                            Log.d(TAG, "New location received: $location")
                            updateMapLocation(location)
                        } else {
                            Log.d(TAG, "Received null location")
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            isTrackingStarted = true
            Toast.makeText(this, "Tracking started!", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Tracking started!")
            binding.btnStart.text = "Stop"

            startTimeTracking()
            handler.postDelayed(stopTrackingRunnable, TRACKING_DURATION_MS)
        }
    }

    private fun stopTracking() {
        val elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis

        if (elapsedTimeMillis < TRACKING_DURATION_MS) {
            val remainingTimeMillis = TRACKING_DURATION_MS - elapsedTimeMillis
            handler.postDelayed(stopTrackingRunnable, remainingTimeMillis)

            mediaPlayer?.start()

            AlertDialog.Builder(this)
                .setMessage("Tracking will stop in ${remainingTimeMillis / 1000 / 60} minutes")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
                .apply {
                    val messageTextView = findViewById<TextView>(android.R.id.message)
                    messageTextView?.setTextColor(Color.BLACK)
                }

        } else {
            mediaPlayer?.start()

            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTrackingStarted = false
            binding.btnStart.text = "Start"
            Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
            binding.chronometer.stop()

            lastChronometerTime = SystemClock.elapsedRealtime() - binding.chronometer.base
            Log.d(TAG, "Last Chronometer Time: $lastChronometerTime")

            timeTrackingHandler.removeCallbacks(timeTrackingRunnable)

            insertData()

            val intent = Intent(this, OxygenLevelActivity::class.java)
            intent.putExtra("source", "BalkeActivity")
            startActivity(intent)
        }
    }

    private fun startTimeTracking() {
        timeTrackingHandler = Handler()
        timeTrackingRunnable = object : Runnable {
            override fun run() {
                if (isTrackingStarted) {
                    lastChronometerTime = SystemClock.elapsedRealtime() - binding.chronometer.base
                    saveLastChronometerTime(lastChronometerTime)
                } else {
                    Log.d(TAG, "Time is not being tracked!")
                }
                timeTrackingHandler.postDelayed(this, timeTrackingIntervalMillis)
            }
        }
        timeTrackingHandler.postDelayed(timeTrackingRunnable, timeTrackingIntervalMillis)
    }

//    private fun updateMapLocation(location: Location) {
//        val currentLatLng = LatLng(location.latitude, location.longitude)
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
//
//        if (allLatLng.size >= 2) {
//            val previousLatLng = allLatLng.last()
//            val distance = calculateDistance(previousLatLng, currentLatLng) / 1000 // in kilometers
//
//            Log.d("Distance Tracker", "distance: $distance")
//
//            if (distance < 0.005) {
//                accumulatedDistance += distance
//
//                Log.d("Distance Tracker", "accumulatedDistance: $accumulatedDistance")
//                if (accumulatedDistance >= 0.1) {
//                    Log.d("Distance Tracker", "totalDistanceMeters: $totalDistanceMeters")
//                    totalDistanceMeters += accumulatedDistance
//
//                    binding.tvDistance.text = String.format(Locale.US, "%.2f", totalDistanceMeters)
//
//                    mMap.addPolyline(
//                        PolylineOptions()
//                            .color(Color.CYAN)
//                            .width(15f)
//                            .add(previousLatLng, currentLatLng)
//                    )
//                    accumulatedDistance = 0.0
//                }
//            }
//        }
//        allLatLng.add(currentLatLng)
//        lastLocation = location
//    }

    private fun updateMapLocation(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

        if (allLatLng.size >= 2) {
            val previousLatLng = allLatLng[allLatLng.size - 2]
            val distance = calculateDistance(
                previousLatLng,
                currentLatLng
            ) / 1000
            totalDistanceMeters += distance
            binding.tvDistance.text = String.format(Locale.US, "%.2f", totalDistanceMeters)

            mMap.addPolyline(
                PolylineOptions()
                    .color(Color.CYAN)
                    .width(15f)
                    .addAll(allLatLng)
            )
        }
        binding.tvDistance.text = String.format(Locale.US, "%.2f", totalDistanceMeters)

        polylinePoints.add(currentLatLng)
        allLatLng.add(currentLatLng)

        lastLocation = location
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

    private fun insertData() {
        val sharedPreferences = getSharedPreferences("user_credentials", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        val userPassword = sharedPreferences.getString("user_password", null)

        val totalDistance = this.totalDistanceMeters // Assuming totalDistance is in kilometers

        Log.d(TAG, "userEmail: $userEmail")
        Log.d(TAG, "userPassword: $userPassword")

        userViewModel.getUserByEmailAndPassword(userEmail.toString(), userPassword.toString())
            .observe(this) { user ->
                val bTrackerId = UUID.randomUUID().toString()
                val userId = user?.userId
                val balkeDuration = lastChronometerTime.toInt()
                val balkeDistance = totalDistance
                val polyLineData = allLatLng.toList() // Convert ArrayList to List
                val currentDate = Date(System.currentTimeMillis())

                if (userId != null) {
                    viewModel.insertDataBalke(
                        bTrackerId,
                        userId,
                        balkeDuration,
                        balkeDistance,
                        polyLineData,
                        currentDate
                    )
                    saveBalkeCredentials(bTrackerId)
                    Log.d(
                        TAG,
                        "Data inserted into ViewModel. bTrackerId: $bTrackerId, UserId: $userId, Duration: $balkeDuration, Distance: $balkeDistance, PolylineDataSize: ${polyLineData.size}, Date: $currentDate"
                    )
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

    private fun saveLastChronometerTime(time: Long) {
        val sharedPrefs = getSharedPreferences("chronometer_prefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong("last_chronometer_time", time)
        editor.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putDouble("totalDistanceMeters", totalDistanceMeters)
        outState.putLong("lastChronometerTime", lastChronometerTime)
        outState.putBoolean("isTrackingStarted", isTrackingStarted)
        outState.putParcelableArrayList("allLatLng", allLatLng)
        super.onSaveInstanceState(outState)
    }

    private fun smoothLocation(previousLocation: Location?, newLocation: Location): Location {
        val smoothedLocation = Location(newLocation)
        if (previousLocation != null) {
            val distance = newLocation.distanceTo(previousLocation)

            if (distance < 200) {
                smoothedLocation.latitude =
                    previousLocation.latitude + (newLocation.latitude - previousLocation.latitude)
                smoothedLocation.longitude =
                    previousLocation.longitude + (newLocation.longitude - previousLocation.longitude)
            } else {
                smoothedLocation.latitude = newLocation.latitude
                smoothedLocation.longitude = newLocation.longitude
            }
        }
        return smoothedLocation
    }

    override fun onResume() {
        super.onResume()
        showCurrentLocation()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_ENABLE_GPS = 2
        private const val TRACKING_DURATION_MS = 900000L
        private const val MIN_MOVEMENT_THRESHOLD = 3
        private const val TAG = "Final RockPort"
    }
}