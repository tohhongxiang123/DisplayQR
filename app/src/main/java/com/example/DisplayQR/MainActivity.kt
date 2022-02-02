package com.example.DisplayQR

import android.R.attr
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.BitmapFactory
import android.util.Base64

import android.graphics.ImageFormat

import android.graphics.YuvImage

import android.graphics.Bitmap
import android.media.Image.Plane
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.widget.Button
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.DisplayQR.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.util.TimerTask

import java.util.Timer
import kotlin.collections.HashMap
import org.json.JSONObject

import com.android.volley.AuthFailureError

import com.android.volley.VolleyError
import android.R.attr.password
import com.android.volley.RetryPolicy

import android.R.string.no











class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
//    private lateinit var btnTakePhoto: Button
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        tvMessage = findViewById(R.id.tvMessage)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS)
        }

//        btnTakePhoto = findViewById(R.id.btnTakePhoto)
//        btnTakePhoto.setOnClickListener {
//            takePhoto()
//        }

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                takePhoto()
            }
        }, 0, 5000)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                mPreview ->
                    mPreview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.d(Constants.TAG, "Start Camera Failed:", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = Constants.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory() : File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object :
            ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeOptInUsageError")
                override fun onCaptureSuccess(image: ImageProxy) {
                    val myBitmap = decodeBitmap(image)
                    val imageData = myBitmap?.let { encodeToByteArray(it) }

                    val queue = Volley.newRequestQueue(this@MainActivity)
                    val url = "https://tohhongxiang.pythonanywhere.com/predict/NTU - N3 AND N4 CLUSTER".replace(" ", "%20")

                    val request = object : VolleyFileUploadRequest(
                        Method.POST,
                        url,
                        Response.Listener {
                            Toast.makeText(this@MainActivity, "Success $it", Toast.LENGTH_LONG).show()
                        },
                        Response.ErrorListener {
                            Toast.makeText(this@MainActivity, "Error $it", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        override fun getByteData(): MutableMap<String, FileDataPart> {
                            var params = HashMap<String, FileDataPart>()
                            params["image"] = FileDataPart("image", imageData!!, "jpeg")
                            return params
                        }
                    }

                    request.retryPolicy = object : RetryPolicy {
                        override fun getCurrentTimeout(): Int {
                            return 50000
                        }

                        override fun getCurrentRetryCount(): Int {
                            return 50000
                        }

                        @Throws(VolleyError::class)
                        override fun retry(error: VolleyError) {
                        }
                    }

                    // Add the request to the RequestQueue.
                    queue.add(request)

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    tvMessage.text = "Error has occurred ${exception.message}"
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun decodeBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun encodeToByteArray(image: Bitmap): ByteArray? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b = baos.toByteArray()
        return b
    }
}