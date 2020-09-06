package com.mho.android.hselfiecamera.features.face

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import com.mho.android.hselfiecamera.usecases.SaveImageIntoGalleryUseCase
import com.mho.android.hselfiecamera.utils.Constants.BUNDLE_KEY_LENS_TYPE
import com.mho.android.hselfiecamera.utils.Constants.EXTRA_NAME_DETECT_MODE_CODE
import com.mho.android.hselfiecamera.utils.Constants.EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE
import com.mho.android.hselfiecamera.utils.getViewModel
import kotlinx.android.synthetic.main.activity_live_face_camera.*
import java.io.IOException

class LiveFaceCameraActivity : AppCompatActivity() {

    private var analyzer: MLFaceAnalyzer? = null
    private var lensEngine: LensEngine? = null
    private var lensType = LensEngine.FRONT_LENS
    private var detectModeCode = 0
    private val smilingRate = 0.8F
    private val smilingPossibility = 0.95F
    private var isSafeToTakePicture = false

    private val saveImageIntoGalleryUseCase: SaveImageIntoGalleryUseCase by lazy {
        SaveImageIntoGalleryUseCase(contentResolver)
    }

    private val liveFaceCameraViewModel: LiveFaceCameraViewModel by lazy {
        getViewModel { LiveFaceCameraViewModel(saveImageIntoGalleryUseCase) }
    }

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

        facingSwitch.setOnClickListener { view ->
            lensType = if(lensType == LensEngine.FRONT_LENS){
                LensEngine.BACK_LENS
            }else{
                LensEngine.FRONT_LENS
            }

            lensEngine?.close()

            startPreview()
        }
        restart.setOnClickListener { startPreview() }

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
                        handler.sendEmptyMessage(ACTION_TAKE_PHOTO)
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
                        isSafeToTakePicture = false
                        handler.sendEmptyMessage(ACTION_TAKE_PHOTO)
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
            analyzer?.setTransactor(object: MLAnalyzer.MLTransactor<MLFace> {
                override fun transactResult(result: MLAnalyzer.Result<MLFace>) {
                    val faceSparseArray = result.analyseList
                    var flag = 0
                    for(i in 0 until faceSparseArray.size()){
                        val emotion = faceSparseArray.valueAt(i).emotions
                        if(emotion.smilingProbability >= smilingPossibility){
                            flag++
                        }
                    }

                    if(flag > faceSparseArray.size() * smilingRate && isSafeToTakePicture){
                        isSafeToTakePicture = false
                        handler.sendEmptyMessage(ACTION_TAKE_PHOTO)
                    }
                }

                override fun destroy() { }
            })
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

                isSafeToTakePicture = true
            }catch (e: IOException){
                lensEngine?.release()
                lensEngine = null
            }
        }
    }

    private fun startPreview(){
        preview.release()
        createFaceAnalyzer()
        createLensEngine()
        startLensEngine()
    }

    private fun stopPreview(){
        restart.visibility = View.VISIBLE
        lensEngine?.release()
        isSafeToTakePicture = false

        if(analyzer != null){
            try {
                analyzer!!.stop()
            }catch(e: IOException){
                Log.e("Error: ", "It's not possible to stop the camera. :(")
            }
        }
    }

    private fun takePhoto(){
        lensEngine?.photograph(null) { bytes ->
            handler.sendEmptyMessage(ACTION_STOP_PREVIEW)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            liveFaceCameraViewModel.onSaveImageIntoGallery(bitmap)
        }
    }

    private val handler: Handler = object: Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                ACTION_STOP_PREVIEW -> stopPreview()
                ACTION_TAKE_PHOTO -> takePhoto()
                else -> { }
            }
        }
    }

    companion object {

        private const val ACTION_STOP_PREVIEW = 1
        private const val ACTION_TAKE_PHOTO = 2
    }
}
