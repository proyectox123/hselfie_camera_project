package com.mho.android.hselfiecamera.utils

import android.content.Context

object CommonUtils {

    fun fromDpToPx(context: Context, dpValue: Float) =
        dpValue * context.resources.displayMetrics.density + 0.5f

}
