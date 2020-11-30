package studio.zewei.willy.customcontrolscamerasample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main_permission.*
import kotlinx.android.synthetic.main.camera_control_layout.*
import kotlinx.android.synthetic.main.dialog_photo_show.view.*
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 111
        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions(this)) {
            setContentView(R.layout.activity_main_permission)
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
            permissionTv.setOnClickListener {
                requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
            }
            return
        }
        setContentView(R.layout.activity_main)

        // Init CameraView
        cameraView.initCameraWithOwner(this, R.layout.camera_control_layout) {
            // Callback after the views to be properly laid out
            val controlsView = cameraView.controlsView

            controlsView?.apply {
                captureBtn.setOnClickListener {
                    cameraView.capture(getCaptureFile()) {
                        // show dialog on ui thread
                        runOnUiThread { showPhotoDialog(it) }
                    }
                }

                lenSwitchBtn.setOnClickListener {
                    cameraView.switchLensFacing()
                }

                flashlightSwitchBtn.setOnClickListener {
                    if (cameraView.isNeedFlashlight) {
                        cameraView.isNeedFlashlight = false
                        flashlightSwitchBtn.setImageResource(R.drawable.ic_camera_no_light)
                    } else {
                        cameraView.isNeedFlashlight = true
                        flashlightSwitchBtn.setImageResource(R.drawable.ic_camera_light)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                // restart
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCaptureFile(): File {
        val path = this.externalMediaDirs.firstOrNull()?.absolutePath + "/"
        val name = System.currentTimeMillis().toString() + ".jpeg"

        val file = File(path + name)
        file.parentFile?.mkdirs()
        file.createNewFile()

        return file
    }

    private fun showPhotoDialog(uri: Uri) {
        val view =
            LayoutInflater.from(this).inflate(R.layout.dialog_photo_show, null, false)

        Glide.with(this)
            .load(uri)
            .into(view.dialogPhoto)

        val builder = AlertDialog.Builder(this, R.style.TransparentBgDialog)
        builder.setView(view)

        val dialog = builder.create()
        dialog.show()

        view.setOnClickListener { dialog.dismiss() }
    }
}