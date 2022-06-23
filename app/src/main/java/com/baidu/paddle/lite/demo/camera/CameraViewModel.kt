package com.baidu.paddle.lite.demo.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.baidu.paddle.lite.demo.AllRet
import com.baidu.paddle.lite.demo.OcrInfo
import com.baidu.paddle.lite.demo.RKApplication
import com.baidu.paddle.lite.demo.base.BaseViewModel
import com.baidu.paddle.lite.demo.common.BackgroundGLSurfaceView
import com.baidu.paddle.lite.demo.common.CommonUtils
import com.baidu.paddle.lite.demo.common.Utils
import com.baidu.paddle.lite.demo.common.UtilsYolov5V
import com.baidu.paddle.lite.demo.ppocr_demo.Native
import com.baidu.paddle.lite.demo.ppocr_demo.SettingsActivity
import com.baidu.paddle.lite.demo.ppocr_demo.UnityNative
import com.rokid.axr.phone.glasscamera.RKGlassCamera
import com.rokid.axr.phone.glasscamera.callback.OnGlassCameraConnectListener
import com.rokid.logger.RKLogger
import com.rokid.utils.ArrayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log

class CameraViewModel : BaseViewModel() {

    private  val TAG = "CameraViewModel"
    private lateinit var model: CameraModel
    var predictor = Native()
//    var unityNative = UnityNative()

    protected var detModelPath = "ch_ppocr_mobile_v2.0_det_slim_opt.nb"
    protected var recModelPath = "ch_ppocr_mobile_v2.0_rec_slim_opt.nb"
    protected var clsModelPath = "ch_ppocr_mobile_v2.0_cls_slim_opt.nb"
    protected var labelPath = "ppocr_keys_v1.txt"
    protected var configPath = "config.txt"
    protected var cpuThreadNum = 1
    protected var cpuPowerMode = "LITE_POWER_HIGH"


    //surface to show preview
    private var surface: Surface? = null
    val previewData: MutableLiveData<ByteArray> = MutableLiveData()
    val painterView: MutableLiveData<Array<OcrInfo>> = MutableLiveData()


    //Camera Connection listener, to open camera immediately when camera is connected.
    private val cameraConnectionListener: OnGlassCameraConnectListener by lazy {
        object : OnGlassCameraConnectListener {
            override fun onGlassCameraConnected(p0: UsbDevice?) {
                //logout camera information on screen when connection is created
                model.showCameraInfo.postValue(true)
                //enable to click preview button after connected to camera
                model.previewEnabled.postValue(true)

                RKGlassCamera.getInstance().openCamera()

            }

            override fun onGlassCameraDisconnected() {
                model.showCameraInfo.postValue(true)
                model.cameraInfo.postValue("Camera Lost!!")
                stopPreview()
                //disable preview button
                model.previewEnabled.postValue(false)
            }

        }
    }

    /**
     * Get model
     * get data model binding with view.
     * @return [CameraModel]
     */
    fun getModel(): CameraModel {
        model = CameraModel { it, toDo ->
            when (it) {
                CameraAction.StartPreview -> {//want to start preview
                    startPreView()
                }
                CameraAction.StopPreview -> {//want to stop preview
                    RKGlassCamera.getInstance().stopPreview()
                }
            }
        }

        return model
    }


    /**
     * Start pre view
     * first of all make sure surface is not null.
     */
    private fun startPreView() {
        RKGlassCamera.getInstance().startPreview(surface, 1920, 1080)

//        surface?.let {
//            RKGlassCamera.getInstance().startPreview(it, 1920, 1080)
//        } ?: run {
//            startPreView()
//        }

    }
    var shownPages = 0
    var startTimeMillion: Long = 0
    /**
     * Start camera
     * This part is just to init camera and start connect with init function.
     */
    fun startCamera() {
        //previewWidth and previewHeight must be set within the supported sizes.
        //supported sizes( For Air Pro ):
        // 1920 * 1080     1280 * 720     640 * 480      800 * 600
        // 1024 * 768      1280 * 960     1600 * 1200    2048 * 1536
        // 2592 * 1944     3264 * 2448    3200 * 2400    2688 * 1512
        // 320 * 240
        RKGlassCamera.getInstance().init(cameraConnectionListener)

        RKGlassCamera.getInstance().setCameraCallback(object : RKGlassCamera.RokidCameraCallback {

            override fun onOpen() {//when camera is opened

            }

            override fun onClose() {
                //in this test nothing to do
            }

            override fun onStartPreview() {//when preview is started.

                model.showCameraInfo.postValue(null)
                //set a default exposure
                val exposure = RKGlassCamera.getInstance().exposure
                RKLogger.e("AE == $exposure")
                //start fps count
                startTimeMillion = System.currentTimeMillis()
                shownPages = 0
                //set a default zoom level
                //Here, after preview is started, supported preview sizes can be got from RKGlassCamera.getInstance().supportedPreviewSizes
                RKGlassCamera.getInstance().supportedPreviewSizes?.forEach {
                    RKLogger.e("it = ${it.width}  *  ${it.height}")
                }

            }

            override fun onStopPreview() {//when stopped.

            }

            override fun onError(p0: Exception?) {//when some error.
                p0?.printStackTrace()
            }

            override fun onStartRecording() {
            }

            override fun onStopRecording() {
            }

        })

        RKGlassCamera.getInstance().addOnPreviewFrameListener { byteArrayValue, timeStamp ->
            //add this interface to use images provided by preview, also timestamp can be find.
            shownPages ++
            val current = System.currentTimeMillis()
            val fps = ((shownPages * 1000) / (current - startTimeMillion))
            model.fps.postValue("FPS: $fps")
            previewData.postValue(byteArrayValue)

        }
    }

    fun getSurfaceListener(): TextureView.SurfaceTextureListener {
        return object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                p0: SurfaceTexture,
                p1: Int,
                p2: Int
            ) {//make sure surface is created after texture is available
                surface = Surface(p0)
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                //in this test nothing to do
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                if (RKGlassCamera.getInstance().isRecording) {
                    RKGlassCamera.getInstance().stopRecord()
                }
                //If texture is destroyed stop preview.
                stopPreview()

                return true
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                //in this test nothing to do
            }

        }
    }

    private fun stopPreview() {
        try {
            RKGlassCamera.getInstance().stopPreview()
        } catch (e: Exception) {
        }
        surface = null
    }

    fun clearModel() {
        model.clearData()
    }

    fun initEngine() {

        val context  = RKApplication.appContext
        val fileDir: String? =context .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        CommonUtils.copyAssetsDirToSDCard(context, "fonts", fileDir)

        initSettings(context)


    }

    private fun initSettings(context:Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.commit()
        SettingsActivity.resetSettings()
    }

    fun loadEngine() {
//        unityNative.init()
        checkRun()
        checkAndUpdateSettings()

    }
    fun checkRun(){
        try {
            val context = RKApplication.appContext
            Utils.copyAssets(context, labelPath)
            val labelRealDir: String = File(context.getExternalFilesDir(null), labelPath).absolutePath
            Utils.copyAssets(context, configPath)
            val configRealDir: String = File(context.getExternalFilesDir(null), configPath).absolutePath
            Utils.copyAssets(context, detModelPath)
            val detRealModelDir: String = File(context.getExternalFilesDir(null), detModelPath).absolutePath
            Utils.copyAssets(context, clsModelPath)
            val clsRealModelDir: String = File(context.getExternalFilesDir(null), clsModelPath).absolutePath
            Utils.copyAssets(context, recModelPath)
            val recRealModelDir: String = File(context.getExternalFilesDir(null), recModelPath).absolutePath
            predictor.init(
                context,
                detRealModelDir,
                clsRealModelDir,
                recRealModelDir,
                configRealDir,
                labelRealDir,
                4,
                cpuPowerMode
            )

//            unityNative.init_ocr(unityNative.getCtx(), detRealModelDir,
//                clsRealModelDir,
//                recRealModelDir,
//                configRealDir,
//                labelRealDir,
//                cpuThreadNum,
//                cpuPowerMode)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun checkAndUpdateSettings() {
        val context = RKApplication.appContext
        if (SettingsActivity.checkAndUpdateSettings(context)) {
            val realModelDir: String = context.cacheDir.toString() + "/" + SettingsActivity.modelDir
            UtilsYolov5V.copyDirectoryFromAssetsYolov5V(
                context,
                SettingsActivity.modelDir,
                realModelDir
            )
            val realLabelPath: String = context.cacheDir.toString() + "/" + SettingsActivity.labelPath
            UtilsYolov5V.copyFileFromAssetsYolov5V(context, SettingsActivity.labelPath, realLabelPath)
            predictor.init_yolov5v(
                realModelDir,
                realLabelPath,
                2,
                SettingsActivity.cpuPowerMode,
                SettingsActivity.inputWidth,
                SettingsActivity.inputHeight,
                SettingsActivity.inputMean,
                SettingsActivity.inputStd,
                SettingsActivity.scoreThreshold
            )
//            unityNative.init_deet(
//                unityNative.getCtx(),
//                realModelDir,
//                realLabelPath,
//                SettingsActivity.cpuThreadNum,
//                SettingsActivity.cpuPowerMode,
//                SettingsActivity.inputWidth,
//                SettingsActivity.inputHeight,
//                SettingsActivity.inputMean,
//                SettingsActivity.inputStd,
//                SettingsActivity.scoreThreshold
//            )
        }
    }

    fun getOnTextureChangedListener() : BackgroundGLSurfaceView.OnTextureChangedListener{

        return object : BackgroundGLSurfaceView.OnTextureChangedListener{
            override fun onTextureChanged(
                inTextureId: Int,
                outTextureId: Int,
                textureWidth: Int,
                textureHeight: Int
            ): Boolean {

                return true;//predictor.process(inTextureId,outTextureId,textureWidth,textureHeight,"")
            }

            override fun onTextureChanged(ARGB8888ImageBitmap: Bitmap?,nv21data: ByteArray): Boolean {
//                viewModelScope.launch(Dispatchers.IO) {
//
//                }
                val process = predictor.process(ARGB8888ImageBitmap,nv21data)
                painterView.postValue(process)
                predictor.process_yolov5v(ARGB8888ImageBitmap, "")
//                val  rets =  unityNative.process(unityNative.getCtx(), ARGB8888ImageBitmap)
//                val ocrs = rets.ocrs
//                val dets = rets.dets
//                Log.e(TAG,"ocr: ${ocrs.size } : dets  ${dets.size}")
//                Log.e(TAG,"ocr: ${ocrs.toString() } : dets  ${dets.toString()}")
                return false
              //  return  predictor.process_yolov5v(ARGB8888ImageBitmap, "")
            }

        }
    }
}