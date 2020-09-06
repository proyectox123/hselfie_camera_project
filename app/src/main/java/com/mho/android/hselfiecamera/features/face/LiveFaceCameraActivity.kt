package com.mho.android.hselfiecamera.features.face

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.utils.Constants.BUNDLE_KEY_LENS_TYPE
import com.mho.android.hselfiecamera.utils.Constants.EXTRA_NAME_DETECT_MODE_CODE
import com.mho.android.hselfiecamera.utils.Constants.EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE
import kotlinx.android.synthetic.main.activity_live_face_camera.*
import java.io.IOException

class LiveFaceCameraActivity : AppCompatActivity() {

    private var analyzer: MLFaceAnalyzer? = null
    private var lensEngine: LensEngine? = null
    private var lensType = LensEngine.FRONT_LENS
    private var detectModeCode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)
        if(savedInstanceState != null){
            lensType = savedInstanceState.getInt(BUNDLE_KEY_LENS_TYPE)
        }

        try {
            detectModeCode = intent.getIntExtra(EXTRA_NAME_DETECT_MODE_CODE, 1)
        }catch (e: RuntimeException) {
            Log.e("Error: ", "No detection code available.")
        }

        createLensEngine()
    }

    override fun onResume() {
        super.onResume()
        startLensEngine()
    }

    override fun onPause() {
        super.onPause()
        preview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lensEngine?.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BUNDLE_KEY_LENS_TYPE, lensType)
        super.onSaveInstanceState(outState)
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

    private fun startLensEngine(){
        restart.visibility = View.GONE
        if(lensEngine != null){
            try {
                if(detectModeCode == EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE){
                    preview.start(lensEngine, faceOverlay)
                }else {
                    preview.start(lensEngine)
                }
            }catch (e: IOException){
                lensEngine?.release()
                lensEngine = null
            }
        }
    }

    private fun startPreview(view: View?){
        preview.release()
        createLensEngine()
        startLensEngine()
    }
}
