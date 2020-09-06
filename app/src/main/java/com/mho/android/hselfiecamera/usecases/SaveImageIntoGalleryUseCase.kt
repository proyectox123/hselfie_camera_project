package com.mho.android.hselfiecamera.usecases

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.mho.android.hselfiecamera.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class SaveImageIntoGalleryUseCase(
    private val contentResolver: ContentResolver
) {

    fun invoke(bitmap: Bitmap) {
        try{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                saveImageIntoGallery(bitmap)
            }else{
                saveImageIntoGalleryPreQ(bitmap)
            }
        }catch (e: Exception) {
            e.printStackTrace()
            Log.e("Error: ", "It's not possible to save the image.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(Exception::class)
    private fun saveImageIntoGallery(bitmap: Bitmap, folderName: String = Constants.DEFAULT_FOLDER_NAME) {
        val values = defaultContentValues().apply {
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            put(MediaStore.Images.Media.IS_PENDING, true)
        }
        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            saveImageToStream(bitmap, contentResolver.openOutputStream(uri))
            values.put(MediaStore.Images.Media.IS_PENDING, false)
            contentResolver.update(uri, values, null, null)
        }
    }

    @Suppress("DEPRECATION")
    @Throws(Exception::class)
    private fun saveImageIntoGalleryPreQ(bitmap: Bitmap, folderName: String = Constants.DEFAULT_FOLDER_NAME) {
        val directory = File(Environment.getExternalStorageDirectory().toString() + File.separator + folderName)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = System.currentTimeMillis().toString() + ".png"
        val file = File(directory, fileName)
        saveImageToStream(bitmap, FileOutputStream(file))

        val values = defaultContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun defaultContentValues() : ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, Constants.MIME_TYPE_IMAGE_PNE)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        }
    }

    @Throws(Exception::class)
    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream == null) return

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }
}
