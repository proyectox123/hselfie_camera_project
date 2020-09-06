package com.mho.android.hselfiecamera.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.huawei.hms.mlsdk.common.MLPosition
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceShape
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.utils.CommonUtils.fromDpToPx

class LocalFaceGraphic(
    private val overlay: GraphicOverlay,
    @field:Volatile private var face: MLFace?,
    private val context: Context
) : BaseGraphic(overlay) {

    private val facePaint: Paint

    init {
        val lineWidth = fromDpToPx(context, 1f)
        facePaint = Paint().apply {
            color = context.resources.getColor(R.color.colorPrimary, context.theme)
            style = Paint.Style.STROKE
            strokeWidth = lineWidth
        }
    }

    override fun draw(canvas: Canvas?) {
        if(face == null){
            return
        }

        val faceShape = face!!.getFaceShape(MLFaceShape.TYPE_FACE)
        val points = faceShape.points
        val verticalRange = Range(0f, Float.MAX_VALUE)
        val horizontalRange = Range(0f, Float.MAX_VALUE)

        fun isNotAValidPoint(point: MLPosition?) =
            point == null || point.x == null || point.y == null

        for(point in points) {
            if(isNotAValidPoint(point)){
                continue
            }

            if(point.x > horizontalRange.max) { horizontalRange.max = point.x }
            if(point.x < horizontalRange.min) { horizontalRange.min = point.x }
            if(point.y > verticalRange.max) { verticalRange.max = point.y }
            if(point.y < verticalRange.min) { verticalRange.min = point.y }

            val rect = Rect(
                translateX(horizontalRange.min).toInt(),
                translateY(verticalRange.min).toInt(),
                translateX(horizontalRange.max).toInt(),
                translateY(verticalRange.max).toInt()
            )
            canvas?.drawRect(rect, facePaint)
        }
    }
}

data class Range(var min: Float, var max: Float)