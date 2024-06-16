package com.example.arm64opencvcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var textViewStatus: TextView
    private lateinit var buttonStartPreview: Button
    private lateinit var buttonStopPreview: Button
    private lateinit var checkBoxProcessing: CheckBox
    private lateinit var imageView: ImageView
    private lateinit var openCvCameraView: CameraBridgeViewBase

    private var isOpenCvInitialized = false
    private var isPreviewActive = false

    private val cameraPermissionRequestCode = 100

    private lateinit var inputMat: Mat
    private lateinit var processedMat: Mat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        textViewStatus = findViewById(R.id.textViewStatus)
        buttonStartPreview = findViewById(R.id.buttonStartPreview)
        buttonStopPreview = findViewById(R.id.buttonStopPreview)
        checkBoxProcessing = findViewById(R.id.checkboxEnableProcessing)
        imageView = findViewById(R.id.imageView)
        openCvCameraView = findViewById(R.id.cameraView)

        isOpenCvInitialized = OpenCVLoader.initLocal()

        // Request access to camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }

        openCvCameraView.setCameraIndex(0)
        openCvCameraView.setCvCameraViewListener(this)

        buttonStartPreview.setOnClickListener {
            openCvCameraView.setCameraPermissionGranted()
            openCvCameraView.enableView()

            updateControls()
        }

        buttonStopPreview.setOnClickListener {
            openCvCameraView.disableView()

            updateControls()
        }

        updateControls()
    }

    private fun updateControls() {
        if(!isOpenCvInitialized) {
            textViewStatus.text = "OpenCV initialization error"

            buttonStartPreview.isEnabled = false;
            buttonStopPreview.isEnabled = false;
        } else {
            textViewStatus.text = "OpenCV initialized"

            buttonStartPreview.isEnabled = !isPreviewActive;
            buttonStopPreview.isEnabled = isPreviewActive;
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        isPreviewActive = true

        inputMat = Mat(height, width, CvType.CV_8UC4)
        processedMat = Mat(height, width, CvType.CV_8UC1)

        updateControls()
    }

    override fun onCameraViewStopped() {
        isPreviewActive = false

        inputMat.release()
        processedMat.release()

        updateControls()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        inputFrame!!.rgba().copyTo(inputMat)

        var matToDisplay = inputMat
        if(checkBoxProcessing.isChecked) {
            Imgproc.cvtColor(inputMat, processedMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.adaptiveThreshold(
                processedMat, processedMat, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 21, 0.0
            )

            matToDisplay = processedMat
        }

        // Prepare the bitmap
        val bitmapToDisplay = Bitmap.createBitmap(matToDisplay.cols(), matToDisplay.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(matToDisplay, bitmapToDisplay)

        // Display it on UI Thread
        runOnUiThread {
            imageView.setImageBitmap(bitmapToDisplay)
        }

        return inputMat
    }
}
