package com.baidu.paddle.lite.demo.main

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.baidu.paddle.lite.demo.RKApplication
import com.rokid.axr.phone.glassdevice.RKGlassDevice

enum class MainActionType {
    Back, NeedPermission
}

class MainModel(
    val topHeight: MutableLiveData<Int> = MutableLiveData(),
    val isAirProPlus: MutableLiveData<Boolean> = MutableLiveData(),
    val action: (MainActionType) -> Unit
) {
    init {
        isAirProPlus.postValue(false)
        isAirProPlus.observeForever {
            if (it == true) {
                //Air Pro + Glass Need Camera Permission when connect.
                action(MainActionType.NeedPermission)
            }else{//
                onNeedConnect()
            }
        }
    }

    fun onBackPressed(v: View) {
        action(MainActionType.Back)
    }

    /**
     * On need connect
     * Do device connect using init function.
     */
    fun onNeedConnect() {
        RKGlassDevice.getInstance().init(RKApplication.INSTANCE.connectListener)
    }
}