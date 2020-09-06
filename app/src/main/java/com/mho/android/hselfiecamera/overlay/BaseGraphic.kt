package com.mho.android.hselfiecamera.overlay

import android.graphics.Canvas
import com.huawei.hms.mlsdk.common.LensEngine

abstract class BaseGraphic(
    private val graphicOverlay: GraphicOverlay
) {

    abstract fun draw(canvas: Canvas?)

    fun scaleX(x: Float): Float = x * graphicOverlay.widthScaleValue

    fun scaleY(y: Float): Float = y * graphicOverlay.heightScaleValue

    fun translateX(x: Float): Float{
        return if(graphicOverlay.cameraFacing == LensEngine.FRONT_LENS){
            graphicOverlay.width - scaleX(x)
        }else{
            scaleX(x)
        }
    }

    fun translateY(y: Float): Float = scaleY(y)
}
