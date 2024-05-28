package com.example.gocekvo2max.heartmonitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gocekvo2max.R
import com.example.gocekvo2max.data.viewmodel.BalkeViewModel
import com.example.gocekvo2max.data.viewmodel.RockPortViewModel
import com.example.gocekvo2max.databinding.ActivityHeartrateBinding
import com.example.gocekvo2max.helper.ImageProcessing
import com.example.gocekvo2max.oxygenlevel.OxygenLevelActivity
import java.util.concurrent.atomic.AtomicBoolean

class HeartRateActivity : AppCompatActivity() {

    enum class TYPE {
        GREEN, RED
    }

    private val TAG = "HeartRateMonitor"
    private val processing = AtomicBoolean(false)
    private var mToast: Toast? = null

    private var previewHolder: SurfaceHolder? = null
    private var camera: Camera? = null

    private lateinit var wakeLock: WakeLock

    private var averageIndex = 0
    private val averageArraySize = 4
    private val averageArray = IntArray(averageArraySize)

    var current = TYPE.GREEN

    private var beatsIndex = 0
    private val beatsArraySize = 3
    private val beatsArray = IntArray(beatsArraySize)
    private var beats = 0.0
    private var startTime: Long = 0

    private var heartLine: StringBuilder = StringBuilder()

    private lateinit var binding: ActivityHeartrateBinding
    private lateinit var rockPortViewModel: RockPortViewModel
    private lateinit var balkeViewModel: BalkeViewModel

    private lateinit var progressBar: ProgressBar

    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartrateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "This is heart rate activity")

        // Check for camera permissions
        if (checkCameraPermission()) {
            initializeCamera()
            Log.d(TAG, "${checkCameraPermission()}")
        } else {
            requestCameraPermission()
        }

        previewHolder = binding.preview.holder

        rockPortViewModel = ViewModelProvider.AndroidViewModelFactory(application)
            .create(RockPortViewModel::class.java)
        balkeViewModel = ViewModelProvider.AndroidViewModelFactory(application)
            .create(BalkeViewModel::class.java)

        previewHolder!!.addCallback(surfaceCallback)
        previewHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "myapp:DoNotDimScreen")

        progressBar = binding.progressBar
        progressBar.max = 100

//        updateData(11111, 11101.toString())
    }

    private fun checkCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
                initializeCamera()
            } else {
                // Camera permission denied
            }
        }
    }

    public override fun onResume() {
        super.onResume()

        wakeLock.acquire(10 * 60 * 1000L)
        camera = Camera.open()
        startTime = System.currentTimeMillis()

        progressBar.visibility = View.VISIBLE
        binding.tvWait.visibility = View.VISIBLE
        progressBar.progress = 0
    }

    public override fun onPause() {
        super.onPause()

        wakeLock.release()

        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null

        runOnUiThread {
            progressBar.visibility = View.INVISIBLE
            binding.tvWait.visibility = View.INVISIBLE
        }
    }

    private val previewCallback = PreviewCallback { data, cam ->
        if (data == null) throw NullPointerException()
        val size = cam.parameters.previewSize ?: throw NullPointerException()

        if (!processing.compareAndSet(false, true)) return@PreviewCallback

        val width = size.width
        val height = size.height
        val imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width)

        if (imgAvg == 255 || imgAvg < 150) {
            showTextToast(getString(R.string.please_place_your_fingertip_on_the_camera))
            processing.set(false)
            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                binding.tvWait.visibility = View.INVISIBLE
            }
            return@PreviewCallback
        }

        var averageArrayAvg = 0
        var averageArrayCnt = 0
        for (i in averageArray.indices) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i]
                averageArrayCnt++
            }
        }

        val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
        var newType = current
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED
            if (newType != current) {
                beats++
                heartLine.append(0, 1, 2, 1)
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN
            heartLine.append(1)
        }

        if (averageIndex == averageArraySize) averageIndex = 0
        averageArray[averageIndex] = imgAvg
        averageIndex++

        // Transitioned from one state to another to the same
        if (newType != current) current = newType

        val endTime = System.currentTimeMillis()
        val totalTimeInSecs = (endTime - startTime) / 1000.0
        if (totalTimeInSecs >= 10) {
            val bps = beats / totalTimeInSecs
            val dpm = (bps * 60.0).toInt()
            if (dpm < 45 || dpm > 145) {
                startTime = System.currentTimeMillis()
                heartLine = StringBuilder()
                beats = 0.0
                processing.set(false)
                return@PreviewCallback
            }

            Log.d(TAG, "totalTimeInSecs=$totalTimeInSecs beats=$beats")
            Log.d(TAG, heartLine.toString())

            if (beatsIndex == beatsArraySize) beatsIndex = 0
            beatsArray[beatsIndex] = dpm
            beatsIndex++

            var beatsArrayAvg = 0
            var beatsArrayCnt = 0
            for (i in beatsArray.indices) {
                if (beatsArray[i] > 0) {
                    beatsArrayAvg += beatsArray[i]
                    beatsArrayCnt++
                }
            }
            val beatsAvg = beatsArrayAvg / beatsArrayCnt

            /** Here what you lookin for **/
            updateData(beatsAvg, heartLine.toString())

            // Start the new activity here
            startActivity(Intent(this@HeartRateActivity, OxygenLevelActivity::class.java))

            setResult(
                10086, Intent()
                    .putExtra("heartBeats", beatsAvg.toString())
                    .putExtra("heartLine", heartLine.toString())
            )
            startTime = System.currentTimeMillis()
            heartLine = StringBuilder()
            beats = 0.0

            runOnUiThread {
                progressBar.visibility = View.INVISIBLE
                binding.tvWait.visibility = View.INVISIBLE
            }

            processing.set(false)
        } else {
            runOnUiThread {
                progressBar.visibility = View.VISIBLE
                binding.tvWait.visibility = View.VISIBLE
            }
        }
        processing.set(false)
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
                camera!!.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
                Log.e("PreviewDemo", "Exception in setPreviewDisplay()", t)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val parameters = camera!!.parameters

            // Check if flash mode is supported
            val supportedFlashModes = parameters.supportedFlashModes
            if (supportedFlashModes != null && supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            } else {
                Log.e(TAG, "Flash mode TORCH is not supported on this device.")
                // Handle the case where TORCH mode is not supported
                return
            }

            val size = getSmallestPreviewSize(width, height, parameters)
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height)
                Log.d(TAG, "Using width=" + size.width + " height=" + size.height)
            }

            try {
                // Set camera parameters
                camera!!.parameters = parameters
                camera!!.startPreview()
            } catch (e: Exception) {
                Log.e(TAG, "Exception setting camera parameters: ${e.message}")
                // Handle the exception as needed
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.d(TAG, "Surface Destroyed")
        }
    }

    private fun showTextToast(msg: String) {
        if (mToast == null) {
            mToast = Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
        }
        mToast!!.show()
    }

    private fun getSmallestPreviewSize(
        width: Int,
        height: Int,
        parameters: Camera.Parameters
    ): Camera.Size? {
        var result: Camera.Size? = null

        for (size in parameters.supportedPreviewSizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size
                } else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height

                    if (newArea < resultArea) result = size
                }
            }
        }

        return result
    }

    private fun initializeCamera() {
        // Your existing camera initialization code
        camera = Camera.open()
        // Rest of your camera initialization logic
    }

    private fun updateData(heartBeats: Int, heartLines: String) {
        val source = intent.getStringExtra("source")
        Log.d(TAG, "Source: $source")

        if (source == "BalkeActivity") {
            val sharedPreferences = getSharedPreferences("balk_credentials", Context.MODE_PRIVATE)
            val balkeId = sharedPreferences.getString("id", null)

            balkeViewModel.getBalkeDataById(balkeId.toString())
                .observeOnce { existingData ->
                    val updatedData = existingData?.copy(
                        heartBeats = heartBeats,
                        heartLines = heartLines
                    )
                    updatedData?.let {
                        balkeViewModel.updateDataBalke(it)
                        Log.d(TAG, "Balke data updated: $it")
                        val intent = Intent(this, OxygenLevelActivity::class.java)
                        intent.putExtra("source", "BalkeActivity")
                        startActivity(intent)
                    }
                }

        } else {
            val sharedPreferences =
                getSharedPreferences("rock_port_credentials", Context.MODE_PRIVATE)
            val rockPortId = sharedPreferences.getString("id", null)

            rockPortViewModel.getRockPortDataById(rockPortId.toString())
                .observeOnce { existingData ->
                    val updatedData = existingData?.copy(
                        heartBeats = heartBeats,
                        heartLines = heartLines
                    )

                    updatedData?.let {
                        rockPortViewModel.updateDataRockPort(it)
                        Log.d(TAG, "Rock port data updated: $it")
                        val intent = Intent(this, OxygenLevelActivity::class.java)
                        intent.putExtra("source", "RockPortActivity")
                        startActivity(intent)
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
}
