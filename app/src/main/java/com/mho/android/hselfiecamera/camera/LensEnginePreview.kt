package com.mho.android.hselfiecamera.camera

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.huawei.hms.common.size.Size
import com.huawei.hms.mlsdk.common.LensEngine
import com.mho.android.hselfiecamera.overlay.GraphicOverlay
import java.io.IOException

class LensEnginePreview: ViewGroup {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val surfaceView: SurfaceView = SurfaceView(context)
    private var startRequested: Boolean = false
    private var isSurfaceAvailable: Boolean = false
    private var lensEngine: LensEngine? = null
    private var overlay: GraphicOverlay? = null

    init {
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        var previewWidth = 320
        var previewHeight = 240
        if(lensEngine != null){
            val size: Size? = lensEngine?.displayDimension
            if(size != null){
                previewHeight = size.height
                previewWidth = size.width
            }
        }

        if(context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            previewWidth -= previewHeight
            previewHeight += previewWidth
            previewWidth = previewHeight - previewWidth
        }

        val viewWidth = right - left
        val viewHeight = bottom - top
        var childXOffset = 0
        var childYOffset = 0
        val widthRatio = viewWidth.toFloat() / previewWidth.toFloat()
        val heightRatio = viewHeight.toFloat() / previewHeight.toFloat()

        val childWidth: Int
        val childHeight: Int
        if(widthRatio > heightRatio){
            childWidth = viewWidth
            childHeight = (previewHeight.toFloat() * heightRatio).toInt()
            childYOffset = (childHeight - viewHeight) / 2
        } else {
            childWidth = (previewWidth.toFloat() * widthRatio).toInt()
            childHeight = viewHeight
            childXOffset = (childWidth - viewWidth) / 2
        }

        (0 until childCount).forEach { i ->
            getChildAt(i).layout(
                -1 * childXOffset,
                -1 * childYOffset,
                childWidth - childXOffset,
                childHeight - childYOffset
            )
        }

        try {
            startIfReady()
        } catch (e: IOException){
            Log.e("Error: ", "It's not possible to initialize the camera.")
        }
    }

    @Throws(IOException::class)
    fun start(lensEngine: LensEngine?){
        if(lensEngine == null){
            stop()
        }

        this.lensEngine = lensEngine
        if(lensEngine != null){
            startRequested = true
        }
    }

    @Throws(IOException::class)
    fun start(lensEngine: LensEngine?, overlay: GraphicOverlay?){
        this.overlay = overlay
        start(lensEngine)
    }

    fun stop() = lensEngine?.close()

    fun release() {
        lensEngine?.release()
        lensEngine = null
    }

    @Throws(IOException::class)
    fun startIfReady(){
        if(startRequested && isSurfaceAvailable){
            lensEngine!!.run(surfaceView.holder)
            if(overlay != null){
                val size: Size = lensEngine!!.displayDimension
                val min: Int = size.width.coerceAtMost(size.height)
                val max: Int = size.width.coerceAtLeast(size.height)
                if(Configuration.ORIENTATION_PORTRAIT == context.resources.configuration.orientation){
                    overlay?.setCameraInfo(min, max, lensEngine!!.lensType)
                }else{
                    overlay?.setCameraInfo(max, min, lensEngine!!.lensType)
                }

                overlay?.clear()
            }

            startRequested = false
        }
    }

    private inner class SurfaceCallback: SurfaceHolder.Callback {

        override fun surfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            isSurfaceAvailable = false
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            isSurfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e("Error: ", "It's not possible to initialize the camera :(")
            }
        }

    }
}
