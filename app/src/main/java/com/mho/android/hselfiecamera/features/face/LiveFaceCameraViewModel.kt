package com.mho.android.hselfiecamera.features.face

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.mho.android.hselfiecamera.usecases.SaveImageIntoGalleryUseCase

class LiveFaceCameraViewModel(
    private val saveImageIntoGalleryUseCase: SaveImageIntoGalleryUseCase
): ViewModel(){

    fun onSaveImageIntoGallery(bitmap: Bitmap){
        saveImageIntoGalleryUseCase.invoke(bitmap)
    }
}
