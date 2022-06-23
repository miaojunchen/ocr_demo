package com.baidu.paddle.lite.demo.camera

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.baidu.paddle.lite.demo.base.BaseEvent
import com.baidu.paddle.lite.demo.ppocr_demo.R
import com.rokid.axr.phone.glasscamera.RKGlassCamera

/**
 * Camera action
 * action type in this view binding data
 */
enum class CameraAction {
    StartPreview, StopPreview
}

/**
 * Camera model
 *
 * @property previewEnabled after glass is connected, post true, default post false.
 * @property showMode post true when camera is connected.
 * @property isAutoFocus if switcher is checked true/false this post a value true/false
 * @property previewSrc text to show on preview button
 * @property action something to do
 */
data class CameraModel(
    val topHeight: MutableLiveData<Int> = MutableLiveData(),
    val fps: MutableLiveData<String> = MutableLiveData(),
    val previewEnabled: MutableLiveData<Boolean> = MutableLiveData(),
    val showMode: MutableLiveData<Boolean> = MutableLiveData(),
    val isAutoFocus: MutableLiveData<Boolean> = MutableLiveData(),
    val previewSrc: MutableLiveData<Int> = MutableLiveData(),
    val showCameraInfo: MutableLiveData<Boolean> = MutableLiveData(),
    val cameraInfo: MutableLiveData<String> = MutableLiveData(),
    val action: (CameraAction, BaseEvent?) -> Unit
) {

    init {//set default values
        previewSrc.postValue(R.string.start_preview)
        showMode.postValue(null)
        previewEnabled.postValue(false)
        isAutoFocus.postValue(true)
        showCameraInfo.postValue(null)
        //set camera focus mode by switcher.
        isAutoFocus.observeForever {
            RKGlassCamera.getInstance().isAutoFocus = it == true
        }


    }

    fun onPreviewClicked(v: View) {
        when (previewSrc.value) {
            R.string.start_preview -> {//to start preview
                action(CameraAction.StartPreview, null)
                previewSrc.postValue(R.string.stop_preview)
                showMode.postValue(true)
            }
            else -> {//to stop preview
                action(CameraAction.StopPreview, null)
                previewSrc.postValue(R.string.start_preview)
                showMode.postValue(null)
            }
        }
    }

    private var threadOn = false
    private var recordShowThread: Thread? = null

    fun clearData() {
        threadOn = false
        recordShowThread = null
        try {
            if (RKGlassCamera.getInstance()?.isRecording == true) {
                RKGlassCamera.getInstance()?.stopRecord()
            }
            RKGlassCamera.getInstance()?.stopPreview()
            if (RKGlassCamera.getInstance()?.isCameraOpened == true) {
                RKGlassCamera.getInstance()?.closeCamera()
            }
            RKGlassCamera.getInstance().deInit()
        } catch (e: Exception) {
        }
    }

}

