package com.example.tennisballtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var infoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.viewFinder)
        infoText = findViewById(R.id.infoText)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), BallAnalyzer())
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class BallAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            // TODO: Implement ball detection & tracking using OpenCV/TFLite
            infoText.text = "Frame: ${'$'}{image.imageInfo.timestamp}"
            image.close()
        }
    }
}
