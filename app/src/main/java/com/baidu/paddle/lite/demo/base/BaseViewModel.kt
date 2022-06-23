package com.baidu.paddle.lite.demo.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baidu.paddle.lite.demo.base.BaseEvent

/**
 * Base view model
 * Based on [ViewModel], include a [MutableLiveData] which can be observed by activities.
 * @constructor
 */
open class BaseViewModel : ViewModel() {
    //event data
    val event: MutableLiveData<BaseEvent> = MutableLiveData()

}