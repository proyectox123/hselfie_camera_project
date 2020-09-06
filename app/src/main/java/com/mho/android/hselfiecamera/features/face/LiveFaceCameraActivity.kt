package com.mho.android.hselfiecamera.features.face

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class LiveFaceCameraActivity : AppCompatActivity() {

    private var analyzer: MLFaceAnalyzer? = null
    private var lensEngine: LensEngine? = null
    private var lensType = LensEngine.FRONT_LENS
    private var detectModeCode = 0
    private val smilingRate = 0.8F
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
            //saveBitmapToGallery(bitmap)
            saveImage(bitmap = bitmap, context = this, folderName = "DCIM/Camera")
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap): String{
        val appDir = File("/storage/emulated/0/DCIM/Camera")
        if(!appDir.exists()){
            if(!appDir.mkdir()){
                Log.e("Error: ", "It's not possible to create the directory.")
                return ""
            }
        }

        val fileName = getString(R.string.app_name) + "_" + System.currentTimeMillis() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            val uri = Uri.fromFile(file)
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        }catch(e: IOException){
            e.printStackTrace()
        }

        return file.absolutePath
    }

    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + File.separator + folderName)

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            val values = contentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
