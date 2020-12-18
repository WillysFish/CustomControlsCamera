package studio.zewei.willy.customcontrolscameraview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView
import com.google.mlkit.vision.face.Face

/**
 * Created by Willy on 2020/12/17.
 */
class FaceContourSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private var faces: MutableList<Face> = mutableListOf()

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 2 * resources.displayMetrics.density
        }
    }

    fun drawFaceRect(faceList: MutableList<Face>) {
        faces = faceList
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            faces.onEach { face -> drawRect(face.boundingBox, paint) }
            save()
        }
    }
}