package com.baidu.paddle.lite.demo.ppocr_demo;

import android.graphics.Bitmap;

import com.baidu.paddle.lite.demo.AllRet;
import com.baidu.paddle.lite.demo.OcrInfo;

public class UnityNative {

    private long ctx = 0;

    static {
        System.loadLibrary("Native");
    }
    public void init()
    {
        ctx = creat();
    }
    public long getCtx()
    {
        return this.ctx;
    }
    public native long creat();

    public native void init_deet(long ctx, String modelDir,
                                String labelPath,
                                int cpuThreadNum,
                                String cpuPowerMode,
                                int inputWidth,
                                int inputHeight,
                                float[] inputMean,
                                float[] inputStd,
                                float scoreThreshold);
    public native  void  init_ocr(long ctx, String detModelPath,
                                  String clsModelPath,
                                  String recModelPath,
                                  String configPath,
                                  String labelPath,
                                  int cputThreadNum,
                                  String cpuPowerMode);
    public native AllRet process(long ctx, Bitmap bitmap);

}
