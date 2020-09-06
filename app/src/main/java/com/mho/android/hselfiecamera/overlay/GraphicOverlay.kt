package com.mho.android.hselfiecamera.overlay

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.mho.android.hselfiecamera.camera.CameraConfiguration

class GraphicOverlay: View {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val lock = Any()
    private var previewWidth = 0
    private var previewHeight = 0
    var widthScaleValue = 1.0f
        private set
    var heightScaleValue = 1.0f
        private set
    var cameraFacing = CameraConfiguration.CAMERA_FACING_FRONT
        private set
    private val graphics: MutableList<BaseGraphic> = ArrayList()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        synchronized(lock) {
            if(previewWidth != 0 && previewHeight != 0){
                widthScaleValue = width.toFloat() / previewWidth.toFloat()
                heightScaleValue = height.toFloat() / previewHeight.toFloat()
            }

            graphics.forEach { graphic -> graphic.draw(canvas) }
        }
    }

    fun addGraphic(graphic: BaseGraphic){
        synchronized(lock) { graphics.add(graphic) }
    }

    fun clear(){
        synchronized(lock) { graphics.clear() }
        this.postInvalidate()
    }

    fun setCameraInfo(width: Int, height: Int, facing: Int){
        synchronized(lock){
            previewWidth = width
            previewHeight = height
            cameraFacing = facing
        }
        this.postInvalidate()
    }
}
