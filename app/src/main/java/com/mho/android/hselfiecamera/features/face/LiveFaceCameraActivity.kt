package com.mho.android.hselfiecamera.features.face

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLResultTrailer
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.overlay.LocalFaceGraphic
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
    private val smilingPossibility = 0.95F
    private var isSafeToTakePicture = false

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

        createFaceAnalyzer()
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
        val context = applicationContext
        lensEngine = LensEngine.Creator(context, analyzer)
            .setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0F)
            .enableAutomaticFocus(true)
            .create()

    }

    private fun createFaceAnalyzer(){
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1F)
            .setTracingAllowed(true)
            .create()

        analyzer = MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)
        if(detectModeCode == EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE){
            val transactor = MLMaxSizeFaceTransactor.Creator(analyzer, object: MLResultTrailer<MLFace?>(){
                override fun objectCreateCallback(itemId: Int, obj: MLFace?) {
                    faceOverlay.clear()
                    if(obj == null){
                        return
                    }

                    val faceGraphic = LocalFaceGraphic(faceOverlay, obj, this@LiveFaceCameraActivity)
                    faceOverlay.addGraphic(faceGraphic)

                    val emotion = obj.emotions
                    if(emotion.smilingProbability >= smilingPossibility){
                        isSafeToTakePicture = true
                    }
                }

                override fun objectUpdateCallback(var1: MLAnalyzer.Result<MLFace?>?, obj: MLFace?) {
                    faceOverlay.clear()
                    if(obj == null){
                        return
                    }

                    val faceGraphic = LocalFaceGraphic(faceOverlay, obj, this@LiveFaceCameraActivity)
                    faceOverlay.addGraphic(faceGraphic)

                    val emotion = obj.emotions
                    if(emotion.smilingProbability >= smilingPossibility && isSafeToTakePicture){
                        isSafeToTakePicture = true
                    }
                }

                override fun lostCallback(result: MLAnalyzer.Result<MLFace?>?) {
                    faceOverlay.clear()
                }

                override fun completeCallback() {
                    faceOverlay.clear()
                }
            }).create()
            analyzer?.setTransactor(transactor)
        } else {

        }
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
        createFaceAnalyzer()
        createLensEngine()
        startLensEngine()
    }
}
