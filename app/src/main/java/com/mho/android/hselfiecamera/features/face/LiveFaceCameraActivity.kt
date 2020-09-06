package com.mho.android.hselfiecamera.features.face

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.mho.android.hselfiecamera.R

class LiveFaceCameraActivity : AppCompatActivity() {

    private var analyzer: MLFaceAnalyzer? = null
    private var lensEngine: LensEngine? = null
    private var lensType = LensEngine.FRONT_LENS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)
        if(savedInstanceState != null){
            lensType = savedInstanceState.getInt("lensType")
        }

        createLensEngine()
    }

    private fun createLensEngine(){
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1F)
            .setTracingAllowed(true)
            .create()

        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)

        val context = applicationContext
        lensEngine = LensEngine.Creator(context, analyzer)
            .setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0F)
            .enableAutomaticFocus(true)
            .create()

    }
}
