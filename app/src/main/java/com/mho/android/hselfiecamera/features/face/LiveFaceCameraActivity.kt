package com.mho.android.hselfiecamera.features.face

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.huawei.hms.mlsdk.face.MLFace
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.overlay.LocalFaceGraphic
import com.mho.android.hselfiecamera.usecases.SaveImageIntoGalleryUseCase
import com.mho.android.hselfiecamera.utils.getViewModel
import kotlinx.android.synthetic.main.activity_live_face_camera.*

class LiveFaceCameraActivity : AppCompatActivity() {

    private val saveImageIntoGalleryUseCase: SaveImageIntoGalleryUseCase by lazy {
        SaveImageIntoGalleryUseCase(contentResolver)
    }

    private val liveFaceCameraViewModel: LiveFaceCameraViewModel by lazy {
        getViewModel {
            LiveFaceCameraViewModel(
                application,
                saveImageIntoGalleryUseCase,
                { obj -> addGraphicToOverlay(obj) },
                { clearOverlay() }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_face_camera)

        liveFaceCameraViewModel.onValidateSavedInstanceState(savedInstanceState)
        liveFaceCameraViewModel.onValidateExtras(intent)

        liveFaceCameraViewModel.events.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { validateNavigation(it) }
        })

        facingSwitch.setOnClickListener { liveFaceCameraViewModel.onFacingSwitch() }
        restart.setOnClickListener { liveFaceCameraViewModel.onStartPreview() }

        liveFaceCameraViewModel.onStartFaceAnalyzer()
    }

    override fun onResume() {
        super.onResume()
        liveFaceCameraViewModel.onStartLensEngine()
    }

    override fun onPause() {
        super.onPause()
        preview.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        liveFaceCameraViewModel.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun clearOverlay(){
        faceOverlay.clear()
    }

    private fun addGraphicToOverlay(obj: MLFace){
        val faceGraphic = LocalFaceGraphic(faceOverlay, obj, this@LiveFaceCameraActivity)
        faceOverlay.addGraphic(faceGraphic)
    }

    private fun validateNavigation(navigation: LiveFaceCameraViewModel.LiveFaceCameraNavigation) {
        when (navigation) {
            LiveFaceCameraViewModel.LiveFaceCameraNavigation.ShowRestartOption -> {
                restart.visibility = View.VISIBLE
            }
            LiveFaceCameraViewModel.LiveFaceCameraNavigation.ReleasePreview -> {
                preview.release()
            }
            LiveFaceCameraViewModel.LiveFaceCameraNavigation.HideRestartOption -> {
                restart.visibility = View.GONE
            }
            is LiveFaceCameraViewModel.LiveFaceCameraNavigation.StartPreviewMostPeople -> navigation.run {
                preview.start(lensEngine)
            }
            is LiveFaceCameraViewModel.LiveFaceCameraNavigation.StartPreviewNearestPeople -> navigation.run {
                preview.start(lensEngine, faceOverlay)
            }
        }
    }
}
