package com.example.cameraprovider

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.cameraprovider.databinding.FragmentCameraBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    lateinit var viewBinding: FragmentCameraBinding

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    lateinit var cameraSelector: CameraSelector

    lateinit var cameraControl: CameraControl

    lateinit var cameraInfo: CameraInfo

    private var currentFlashMode = ImageCapture.FLASH_MODE_OFF

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    context,
                    "Quyền truy cập máy ảnh đã bị từ chối",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// kiem tra xem tat ca quyen da ok chua
        if (allPermissionsGranted()) {
            startCamera()

        } else {
            requestPermissions()
        }

        viewBinding.buttonCapture.setOnClickListener {
            takePhoto()
        }

        // Set up the listeners for take photo and video capture buttons

        viewBinding.buttonSwitchCamera.setOnClickListener {
            swipcam()
        }
        viewBinding.buttonFlash.setOnClickListener {
            flash()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun updateViewFinderAspectRatio(aspectRatio: String) {

    }


    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        }
    }


    private fun flash() {
        if (cameraProvider == null) {
            Log.e(TAG, "cameraprovider chưa được khởi tạo. Không thể bật flash")
            return
        }
        currentFlashMode = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            else -> throw IllegalStateException("Unexpected")
        }
        imageCapture?.setFlashMode(currentFlashMode)
    }

    private fun capquyencam() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Bật quyền truy cập Máy ảnh")
            .setMessage("Đến cài đặt ứng dụng và đảm bảo Close có quyền truy cập máy ảnh của bạn")
            .setPositiveButton("ĐẾN CÀI ĐẶT") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("HỦY") { dialog, _ ->
                dialog.dismiss()
            }
            .show()


    }

    private fun takePhoto() {

        if (!allPermissionsGranted()) {
            capquyencam()
        }

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Close")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext().applicationContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    viewBinding.imageViewCaptured.apply {
                        visibility = View.VISIBLE
                        viewBinding.viewFinder.visibility = View.INVISIBLE
                        setImageURI(savedUri)
                    }

                    cameraProvider.unbindAll()

                    viewBinding.apply {
                        btnLeft.visibility = View.VISIBLE
                        edt1.visibility = View.VISIBLE
                        fncLauout.visibility = View.INVISIBLE
                        btnPost.visibility = View.VISIBLE
                        buttonCapture.visibility = View.GONE
                    }



                    viewBinding.btnPost.setOnClickListener {
                        val i = Intent(Intent.ACTION_SEND)
                        i.type = "image/*"
                        i.putExtra(Intent.EXTRA_STREAM, savedUri)
                        startActivity(i)
                    }
                }
            }
        )
    }

    private fun swipcam() {
        if (cameraProvider == null) {
            Log.e(TAG, "cameraprovider chưa được khởi tạo. Không thể xoay cam")
            return
        }

        cameraSelector = when (cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalStateException("Unexpected")
        }

        try {
            // Unbind all existing use cases before rebinding with the new camera
            cameraProvider.unbindAll()

            bindCameraUseCases()

        } catch (exc: Exception) {
            Log.e(TAG, "Camera use case binding failed during switch", exc)
        }
    }

    fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext().applicationContext)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                bindCameraUseCases()

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext().applicationContext))
    }

    private fun bindCameraUseCases() {
        val targetResolution = Size(1080, 1080)
        val preview = Preview.Builder()
            .setTargetResolution(targetResolution)
            .build()
            .also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setTargetResolution(targetResolution)
            .setFlashMode(currentFlashMode)
            .setJpegQuality(100)
            .build()
        // Bind the preview and image capture use cases to the camera
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider.unbindAll()


    }

    fun getCameraProvider(): ProcessCameraProvider? {
        return cameraProvider
    }

    companion object {

        private lateinit var cameraProvider: ProcessCameraProvider

        fun setCameraProvider(provider: ProcessCameraProvider) {
            cameraProvider = provider
        }

        fun getCameraProvider(): ProcessCameraProvider? {
            return if (::cameraProvider.isInitialized) cameraProvider else null
        }

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}