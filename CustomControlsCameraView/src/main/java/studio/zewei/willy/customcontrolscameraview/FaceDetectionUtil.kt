package studio.zewei.willy.customcontrolscameraview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions


/**
 * Created by WillyYan on 2020/12/17.
 */
object FaceDetectionUtil {

    private val detector by lazy { FaceDetection.getClient() }

    private val realTimeDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
        )
    }

    private fun detect(
        detector: FaceDetector,
        image: InputImage,
        resultCallback: (faces: MutableList<Face>?) -> Unit,
        finalCallback: () -> Unit = {}
    ) {
        detector.process(image)
            .addOnSuccessListener {
                resultCallback(it)
            }.addOnFailureListener {
                resultCallback(null)
                Log.e("Face Detector Process Err: ", it.toString())
            }.addOnCompleteListener { finalCallback() }
    }

    private fun detectFace(
        image: InputImage,
        resultCallback: (faces: MutableList<Face>?) -> Unit,
        finalCallback: () -> Unit = {}
    ) = detect(detector, image, resultCallback, finalCallback)


    private fun realTimeDetectFace(
        image: InputImage,
        resultCallback: (faces: MutableList<Face>?) -> Unit,
        finalCallback: () -> Unit = {}
    ) = detect(realTimeDetector, image, resultCallback, finalCallback)


    @SuppressLint("UnsafeExperimentalUsageError")
    fun detectFaceByImageProxy(
        imageProxy: ImageProxy,
        resultCallback: (faces: MutableList<Face>?) -> Unit
    ) {
        imageProxy.image?.apply {
            val image = InputImage.fromMediaImage(this, imageProxy.imageInfo.rotationDegrees)
            realTimeDetectFace(image, resultCallback) { imageProxy.close() }
        }
    }

    private fun hasFace(faces: MutableList<Face>?) = faces?.let { it.size > 0 }

    /**
     * Face Detection for File (jpg、png...)
     */
    fun hasFaceInFile(context: Context, uri: Uri, resultCallback: (has: Boolean?) -> Unit) {
        val image = InputImage.fromFilePath(context, uri)
        detectFace(image, { resultCallback(hasFace(it)) })
    }

    /**
     * Face Detection for Bitmap
     */
    fun hasFaceInBitmap(bitmap: Bitmap, resultCallback: (has: Boolean?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detectFace(image, { resultCallback(hasFace(it)) }) { bitmap.recycle() }
    }
}