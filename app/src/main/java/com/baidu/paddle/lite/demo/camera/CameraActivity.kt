package com.baidu.paddle.lite.demo.camera

import android.Manifest
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.baidu.paddle.lite.demo.OcrInfo
import com.baidu.paddle.lite.demo.base.BaseActivity
import com.baidu.paddle.lite.demo.base.DataBinding
import com.baidu.paddle.lite.demo.ppocr_demo.databinding.ActivityCamera1Binding
import com.baidu.paddle.lite.demo.ppocr_demo.databinding.PreviewSingleBinding
import com.rokid.axr.phone.glasscamera.RKGlassCamera


/**
 * Camera activity
 * Activity created base on [BaseActivity] to test Camera usage of the glass.
 * Make sure that this test is just for [DeviceType.AirPro] & [DeviceType.Glass2] & [DeviceType.AirProPlus]
 *
 */
class CameraActivity : BaseActivity() {
    //viewDataBinding
    private lateinit var dataBinding: ActivityCamera1Binding

    //base on BaseViewModel
    private lateinit var viewModel: CameraViewModel

    private var presentation: Presentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = ActivityCamera1Binding.inflate(layoutInflater)
        dataBinding.lifecycleOwner = this
        setContentView(dataBinding.root)

        viewModel = getViewModel(CameraViewModel::class.java)
        //make sure this is used in every activity based on BaseActivity.
        initViewModel(viewModel)
        //set dataBinding's source data.
        dataBinding.data = viewModel.getModel().apply {
            topHeight.postValue(DataBinding.getStatusBarHeight(this@CameraActivity))
        }
        //this test will use camera and microphone to record videos, so the follows permissions should be confirmed.
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 666
        )
        //use the OnSurfaceListener provided by viewModel
        //to display on glass with presentation, on phone the view is black.
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).let {
            presentation = object : Presentation(this, it.displays[it.displays.size.minus(1)]){
                private lateinit var binding: PreviewSingleBinding
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    binding = PreviewSingleBinding.inflate(layoutInflater)
                    binding.lifecycleOwner = this@CameraActivity
                    setContentView(binding.root)
                    //do things when view is ready
//                    binding.texture.surfaceTextureListener = viewModel.getSurfaceListener()
                    val surface = binding.texture.holder.surface
//                    Log.e("RKGlassCamera.getInstance().surface: ", surface.toString())
//

                    binding.texture.onTextureChangedListener = viewModel.getOnTextureChangedListener()
                    RKGlassCamera.getInstance().startPreview(surface, 1920, 1080)
                    viewModel.previewData.observe(this@CameraActivity){
                        binding.texture.setPreviewData(it,1920, 1080)
                    }
                    viewModel.painterView.observe(this@CameraActivity){ array:Array<OcrInfo>->
                        binding.painter.setResult(array)
                    }
                }
            }
            presentation?.show()


        }
//        //if don't want to use presentation to show Preview

        viewModel.initEngine()

//        dataBinding.preview.onTextureChangedListener = viewModel.getOnTextureChangedListener()
//        val surface = dataBinding.preview.holder.surface
//        RKGlassCamera.getInstance().startPreview(surface, 1920, 1080)
//
//
//        viewModel.previewData.observe(this){
//            dataBinding.preview.setPreviewData(it,1920, 1080)
//        }
//
//        viewModel.painterView.observe(this@CameraActivity){ array:Array<OcrInfo>->
//            dataBinding.painter.setResult(array)
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            666 -> {//for this is a test so every permission is required by default.
                var all = true
                grantResults.forEach {
                    all = (it == PackageManager.PERMISSION_GRANTED) && all
                }
                if (all) {
                    viewModel.startCamera()
                } else {//goto the application settings to request all permissions.
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        data = Uri.fromParts("package", packageName, null)
                    })
                    this.finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEngine()
    }

    override fun onDestroy() {
        viewModel.clearModel()
        //if use presentation to show, close it after use
        presentation?.dismiss()
        super.onDestroy()
    }
}