package studio.zewei.willy.customcontrolscameraview

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.annotation.LayoutRes
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * Created by Willy on 2020/11/10.
 * https://github.com/android/camera-samples/tree/master
 */
class CustomControlsCameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CustomCameraView"
    }

    private val attributes by lazy {
        context.obtainStyledAttributes(attrs, R.styleable.CustomControlsCameraView)
    }
    private val enableFaceDetection =
        attributes.getBoolean(R.styleable.CustomControlsCameraView_enableFaceDetection, false)

    private val container by lazy { this }
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var savedUri: Uri? = null

    private val previewView by lazy {
        val view = PreviewView(context)
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        view
    }

    private val surfaceView by lazy {
        val view = FaceContourSurfaceView(context)
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        view.setBackgroundColor(Color.TRANSPARENT)
        view
    }

    // Create a Executor for camera operations
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    init {
        addView(previewView)

        if (enableFaceDetection)
            addView(surfaceView)

        // Wait for the views to be properly laid out
        previewView.post {
            // Build UI controls
            updateCameraUi()
            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    var controlsView: View? = null
    var isNeedFlashlight = false

    private var lifecycleOwner: LifecycleOwner? = null
    private var controlsLayoutId: Int? = null
    private var initFinishedCallback = {}

    /**
     * Necessarily invoke the function for initial params from owner.
     * */
    fun initCameraWithOwner(
        lifecycleOwner: LifecycleOwner,
        @LayoutRes controlsLayoutId: Int,
        initFinishedCallback: () -> Unit
    ) {
        this.lifecycleOwner = lifecycleOwner
        this.controlsLayoutId = controlsLayoutId
        this.initFinishedCallback = initFinishedCallback
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyView() {
        cameraExecutor.shutdown()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Redraw the camera UI controls when the orientation changed
        updateCameraUi()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            initFinishedCallback()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = previewView.display.rotation

        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .apply {
                if (enableFaceDetection)
                    setAnalyzer(cameraExecutor) { imageProxy ->
                        FaceDetectionUtil.detectFaceByImageProxy(imageProxy) { faces ->
                            faces?.apply { surfaceView.drawFaceRect(this) }
                        }
                    }
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            lifecycleOwner?.apply {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } ?: run { throw IllegalStateException("Need to invoke initCameraWithOwner() first.") }

            // Attach the surface provider
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * Get a suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        // 4:3 vs 16:9 Which one is closer?
        if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        controlsLayoutId?.also { id ->
            container.findViewById<ConstraintLayout>(id)?.let {
                container.removeView(it)
            }
            controlsView = View.inflate(context, id, container)
        } ?: run { throw IllegalStateException("Need to invoke initCameraWithOwner() first.") }
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    fun isFrontLens(): Boolean = CameraSelector.LENS_FACING_FRONT == lensFacing

    // Fake Flashlight for Front Lens.
    private fun openFrontFlashlight(isOpen: Boolean) {
        val activity = (context as Activity)
        if (isOpen) {
            activity.windowBrightness = 1f
            container.foreground = ColorDrawable(Color.WHITE)
        } else {
            activity.windowBrightness = -1f
            container.foreground = null
        }
    }

    fun switchLensFacing(specific: CameraLens? = null) {
        lensFacing =
            specific?.let {
                when (it) {
                    CameraLens.FACING_BACK -> CameraSelector.LENS_FACING_BACK
                    CameraLens.FACING_FRONT -> CameraSelector.LENS_FACING_FRONT
                }
            } ?: if (CameraSelector.LENS_FACING_FRONT == lensFacing)
                CameraSelector.LENS_FACING_BACK
            else
                CameraSelector.LENS_FACING_FRONT

        // Re-bind use cases to update selected camera
        bindCameraUseCases()
    }

    fun capture(photoFile: File, finishAction: (uri: Uri, hasFace: Boolean?) -> Unit) {
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Setup image capture metadata
            val metadata = Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // open flashlight of back lens
            imageCapture.flashMode =
                if (isNeedFlashlight && lensFacing == CameraSelector.LENS_FACING_BACK) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF

            // open flashlight of front lens
            if (isNeedFlashlight && lensFacing == CameraSelector.LENS_FACING_FRONT)
                openFrontFlashlight(true)

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // close flashlight of front len
                        container.post { openFrontFlashlight(false) }

                        savedUri?.also {
                            // return photo and face detection result
                            if (enableFaceDetection)
                                FaceDetectionUtil.hasFaceInFile(context, it) { hasFace ->
                                    finishAction(it, hasFace)
                                }
                            else
                                finishAction(it, null)

                            // MediaScanner for other apps be able to access our images
                            val mimeType = MimeTypeMap.getSingleton()
                                .getMimeTypeFromExtension(it.toFile().extension)
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(it.toFile().absolutePath),
                                arrayOf(mimeType)
                            ) { _, uri ->
                                Log.d(TAG, "Image capture scanned into media store: $uri")
                            }
                        }
                    }
                })

            // Flash animation
            if (!(isNeedFlashlight && lensFacing == CameraSelector.LENS_FACING_FRONT))
                container.postDelayed({
                    container.foreground = ColorDrawable(Color.WHITE)
                    container.postDelayed(
                        { container.foreground = null }, 50
                    )
                }, 100)
        }
    }

    fun getLastPhoto() = savedUri

    fun getDisplayId() = previewView.display.displayId

    /**
     * Setting Rotation for owner
     */
    fun setDisplayRotation(rotation: Int) {
        Log.d(TAG, "Rotation changed: $rotation")
        imageCapture?.targetRotation = rotation
        imageAnalyzer?.targetRotation = rotation
    }
}