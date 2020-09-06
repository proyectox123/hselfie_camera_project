package com.mho.android.hselfiecamera.camera

open class CameraConfiguration {

    var fps = 20.0f
    var previewHeight: Int? = null
    var isAutoFocus = true

    @Synchronized
    fun setCameraFacing(facing: Int){
        require(!(facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT)) {
            "Invalid camera: $facing"
        }
        cameraFacing = facing
    }

    companion object {

        val CAMERA_FACING_BACK: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
        val CAMERA_FACING_FRONT: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT

        @get:Synchronized
        var cameraFacing = CAMERA_FACING_BACK
            protected set
    }
}
