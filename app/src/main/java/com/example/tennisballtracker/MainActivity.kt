package com.example.tennisballtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.SystemClock
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.widget.TextView
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var overlay: BallOverlay
    private lateinit var scoreAText: TextView
    private lateinit var scoreBText: TextView
    private lateinit var speedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.viewFinder)
        overlay = findViewById(R.id.overlay)
        scoreAText = findViewById(R.id.scoreA)
        scoreBText = findViewById(R.id.scoreB)
        speedText = findViewById(R.id.speedText)

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
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    BallTrackerAnalyzer(overlay, scoreAText, scoreBText, speedText)
                )
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private class BallTrackerAnalyzer(
        private val overlay: BallOverlay,
        private val scoreAText: TextView,
        private val scoreBText: TextView,
        private val speedText: TextView,
    ) : ImageAnalysis.Analyzer {
        private var lastPoint: PointF? = null
        private var lastTime: Long = 0L
        private var missCount = 0
        private var scoreA = 0
        private var scoreB = 0

        override fun analyze(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val point = detectBall(bitmap)
            val now = SystemClock.elapsedRealtime()
            var speed = 0f
            if (point != null && lastPoint != null) {
                val dt = (now - lastTime) / 1000f
                val dx = point.x - lastPoint!!.x
                val dy = point.y - lastPoint!!.y
                speed = if (dt > 0) sqrt(dx * dx + dy * dy) / dt else 0f
            }
            if (point != null) {
                lastPoint = point
                lastTime = now
                missCount = 0
            } else {
                missCount++
                if (missCount > 10 && lastPoint != null) {
                    val width = bitmap.width
                    if (lastPoint!!.x < width / 2) scoreB++ else scoreA++
                    lastPoint = null
                }
            }
            overlay.update(point)
            scoreAText.post { scoreAText.text = "A: ${'$'}scoreA" }
            scoreBText.post { scoreBText.text = "B: ${'$'}scoreB" }
            speedText.post { speedText.text = String.format("%.1f px/s", speed) }
            image.close()
        }

        private fun detectBall(bitmap: Bitmap): PointF? {
            val rgba = Mat()
            Utils.bitmapToMat(bitmap, rgba)
            val hsv = Mat()
            Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2HSV)
            val lower = Scalar(20.0, 50.0, 50.0)
            val upper = Scalar(40.0, 255.0, 255.0)
            val mask = Mat()
            Core.inRange(hsv, lower, upper, mask)
            val moments = Imgproc.moments(mask)
            return if (moments.m00 > 0) {
                PointF((moments.m10 / moments.m00).toFloat(), (moments.m01 / moments.m00).toFloat())
            } else null
        }
    }

    companion object {
        init { System.loadLibrary("opencv_java4") }
    }
}
