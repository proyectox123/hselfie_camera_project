package com.mho.android.hselfiecamera.features.face

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLResultTrailer
import com.huawei.hms.mlsdk.face.MLFace
import com.huawei.hms.mlsdk.face.MLFaceAnalyzer
import com.huawei.hms.mlsdk.face.MLFaceAnalyzerSetting
import com.huawei.hms.mlsdk.face.MLMaxSizeFaceTransactor
import com.mho.android.hselfiecamera.features.face.LiveFaceCameraViewModel.LiveFaceCameraNavigation.*
import com.mho.android.hselfiecamera.usecases.SaveImageIntoGalleryUseCase
import com.mho.android.hselfiecamera.utils.Constants
import com.mho.android.hselfiecamera.utils.Constants.EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE
import com.mho.android.hselfiecamera.utils.Event
import java.io.IOException

class LiveFaceCameraViewModel(
    application: Application,
    private val saveImageIntoGalleryUseCase: SaveImageIntoGalleryUseCase,
    private val addGraphicToOverlay: (obj: MLFace) -> Unit,
    private val clearOverlay: () -> Unit
): AndroidViewModel(application){

    val smilingRate = 0.8F
    val smilingPossibility = 0.95F
    var detectModeCode = 0
    var lensType = LensEngine.FRONT_LENS
    var lensEngine: LensEngine? = null
    var isSafeToTakePicture = false

    private val analyzer: MLFaceAnalyzer by lazy {
        val setting = MLFaceAnalyzerSetting.Factory()
            .setFeatureType(MLFaceAnalyzerSetting.TYPE_FEATURES)
            .setKeyPointType(MLFaceAnalyzerSetting.TYPE_UNSUPPORT_KEYPOINTS)
            .setMinFaceProportion(0.1F)
            .setTracingAllowed(true)
            .create()

        MLAnalyzerFactory.getInstance().getFaceAnalyzer(setting)
    }

    private val _events = MutableLiveData<Event<LiveFaceCameraNavigation>>()
    val events: LiveData<Event<LiveFaceCameraNavigation>> get() = _events

    override fun onCleared() {
        super.onCleared()
        lensEngine?.release()
    }

    fun onValidateSavedInstanceState(savedInstanceState: Bundle?) {
        lensType = savedInstanceState?.getInt(Constants.BUNDLE_KEY_LENS_TYPE) ?: LensEngine.FRONT_LENS
    }

    fun onSaveInstanceState(outState: Bundle){
        outState.putInt(Constants.BUNDLE_KEY_LENS_TYPE, lensType)
    }

    fun onValidateExtras(intent: Intent){
        try {
            detectModeCode = intent.getIntExtra(Constants.EXTRA_NAME_DETECT_MODE_CODE, 1)
        }catch (e: RuntimeException) {
            Log.e("Error: ", "No detection code available.")
        }
    }

    fun onTakePhoto(){
        lensEngine?.photograph(null) { bytes ->
            stopPreview()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            saveImageIntoGalleryUseCase.invoke(bitmap)
        }
    }

    fun onStartFaceAnalyzer() {
        createFaceAnalyzer()
        createLensEngine()
    }

    fun onStartPreview(){
        onStartFaceAnalyzer()
        onStartLensEngine()
    }

    fun onStartLensEngine(){
        _events.value = Event(HideRestartOption)
        if(lensEngine != null){
            try {
                if(detectModeCode == EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE){
                    _events.value = Event(StartPreviewNearestPeople(lensEngine))
                }else {
                    _events.value = Event(StartPreviewMostPeople(lensEngine))
                }

                isSafeToTakePicture = true
            }catch (e: IOException){
                lensEngine?.release()
                lensEngine = null
            }
        }
    }

    fun onFacingSwitch(){
        lensType = when (lensType) {
            LensEngine.FRONT_LENS -> LensEngine.BACK_LENS
            else -> LensEngine.FRONT_LENS
        }

        lensEngine?.close()

        onStartPreview()
    }

    private fun createFaceAnalyzer(){
        when (detectModeCode) {
            EXTRA_VALUES_DETECT_MODE_CODE_NEAREST_PEOPLE -> {
                analyzer.setTransactor(transactorNearestPeople())
            }
            else -> {
                analyzer.setTransactor(transactorMostPeople())
            }
        }
    }

    private fun transactorNearestPeople() = MLMaxSizeFaceTransactor.Creator(analyzer, object: MLResultTrailer<MLFace?>(){
        override fun objectCreateCallback(itemId: Int, obj: MLFace?) {
            clearOverlay()
            if(obj == null){
                return
            }

            addGraphicToOverlay(obj)

            val emotion = obj.emotions
            if(emotion.smilingProbability >= smilingPossibility){
                isSafeToTakePicture = true
                onTakePhoto()
            }
        }

        override fun objectUpdateCallback(var1: MLAnalyzer.Result<MLFace?>?, obj: MLFace?) {
            clearOverlay()
            if(obj == null){
                return
            }

            addGraphicToOverlay(obj)

            val emotion = obj.emotions
            if(emotion.smilingProbability >= smilingPossibility && isSafeToTakePicture){
                isSafeToTakePicture = false
                onTakePhoto()
            }
        }

        override fun lostCallback(result: MLAnalyzer.Result<MLFace?>?) {
            clearOverlay()
        }

        override fun completeCallback() {
            clearOverlay()
        }
    }).create()

    private fun transactorMostPeople() = object : MLAnalyzer.MLTransactor<MLFace> {
        override fun transactResult(result: MLAnalyzer.Result<MLFace>) {
            val faceSparseArray = result.analyseList
            val flag = (0 until faceSparseArray.size())
                .map { faceSparseArray.valueAt(it).emotions }
                .count { it.smilingProbability >= smilingPossibility }

            if (flag > faceSparseArray.size() * smilingRate && isSafeToTakePicture) {
                isSafeToTakePicture = false
                onTakePhoto()
            }
        }

        override fun destroy() { }
    }

    private fun createLensEngine(){
        lensEngine = LensEngine.Creator(getApplication(), analyzer)
            .setLensType(lensType)
            .applyDisplayDimension(640, 480)
            .applyFps(25.0F)
            .enableAutomaticFocus(true)
            .create()
    }

    private fun stopPreview(){
        _events.value = Event(ShowRestartOption)
        lensEngine?.release()
        isSafeToTakePicture = false

        try {
            analyzer.stop()
        }catch(e: IOException){
            Log.e("Error: ", "It's not possible to stop the camera. :(")
        }
    }

    sealed class LiveFaceCameraNavigation {
        data class StartPreviewMostPeople(val lensEngine: LensEngine?) : LiveFaceCameraNavigation()
        data class StartPreviewNearestPeople(val lensEngine: LensEngine?) : LiveFaceCameraNavigation()
        object ReleasePreview: LiveFaceCameraNavigation()
        object ShowRestartOption: LiveFaceCameraNavigation()
        object HideRestartOption: LiveFaceCameraNavigation()
    }
}
