package com.baidu.paddle.lite.demo.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import com.baidu.paddle.lite.demo.base.BaseActivity
import com.baidu.paddle.lite.demo.base.DataBinding
import com.baidu.paddle.lite.demo.ppocr_demo.R
import com.baidu.paddle.lite.demo.ppocr_demo.databinding.ActivityMainBinding


/**
 * Main activity
 * This activity is build if first start activity or connection error occurred.
 * @constructor Create empty Main activity
 */
class MainActivity : BaseActivity() {

    private lateinit var dataBinding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    var lastBackPressed: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = ActivityMainBinding.inflate(layoutInflater)
        dataBinding.lifecycleOwner = this
        setContentView(dataBinding.root)
        viewModel = getViewModel(MainViewModel::class.java)
        initViewModel(viewModel)
        dataBinding.data = viewModel.getModel(DataBinding.getStatusBarHeight(this))
    }


    /**
     * On back pressed
     * Double clicked to quite this application.
     */
    override fun onBackPressed() {
        if (System.currentTimeMillis() - lastBackPressed < 1000L) {
            finish()
        } else {
            Toast.makeText(this, getString(R.string.quite), Toast.LENGTH_SHORT).show()
        }
        lastBackPressed = System.currentTimeMillis()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            viewModel.cameraRequest -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startConnect()
                }
            }
        }
    }
}