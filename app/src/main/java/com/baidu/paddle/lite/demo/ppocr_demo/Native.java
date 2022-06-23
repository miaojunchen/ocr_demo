package com.baidu.paddle.lite.demo.ppocr_demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.paddle.lite.demo.OcrInfo;
import com.baidu.paddle.lite.demo.common.SDKExceptions;
import com.baidu.paddle.lite.demo.common.Utils;

public class Native {
    static {
        System.loadLibrary("Native");
    }

    private long ctx = 0;
    private long ctx_yolov5v = 0;


    public boolean init(Context mContext,
                        String detModelPath,
                        String clsModelPath,
                        String recModelPath,
                        String configPath,
                        String labelPath,
                        int cputThreadNum,
                        String cpuPowerMode) {
        ctx = nativeInit(
                detModelPath,
                clsModelPath,
                recModelPath,
                configPath,
                labelPath,
                cputThreadNum,
                cpuPowerMode);
        return ctx == 0;
    }

    public boolean release() {
        if (ctx == 0) {
            return false;
        }
        return nativeRelease(ctx);
    }

    public boolean process(int inTextureId, int outTextureId, int textureWidth, int textureHeight, String savedImagePath) {
        if (ctx == 0) {
            return false;
        }
        return nativeProcess(ctx, inTextureId, outTextureId, textureWidth, textureHeight, savedImagePath);
    }

    public OcrInfo[] process(Bitmap bitmap,byte[] nv21data){
        if (ctx == 0){
            return null;
        }
        return process(ctx,bitmap,nv21data);
    }

    public static native long nativeInit(String detModelPath,
                                         String clsModelPath,
                                         String recModelPath,
                                         String configPath,
                                         String labelPath,
                                         int cputThreadNum,
                                         String cpuPowerMode);

    public static native boolean nativeRelease(long ctx);

    public static native boolean nativeProcess(long ctx, int inTextureId, int outTextureId, int textureWidth, int textureHeight, String savedImagePath);

    //================yolov5v

    public boolean init_yolov5v(String modelDir,
                        String labelPath,
                        int cpuThreadNum,
                        String cpuPowerMode,
                        int inputWidth,
                        int inputHeight,
                        float[] inputMean,
                        float[] inputStd,
                        float scoreThreshold) {
        ctx_yolov5v = nativeInit_yolov5v(
                modelDir,
                labelPath,
                cpuThreadNum,
                cpuPowerMode,
                inputWidth,
                inputHeight,
                inputMean,
                inputStd,
                scoreThreshold);
        return ctx_yolov5v == 0;
    }

    public boolean release_yolov5v() {
        if (ctx_yolov5v == 0) {
            return false;
        }
        return nativeRelease(ctx_yolov5v);
    }

    public boolean process_yolov5v(Bitmap ARGB8888ImageBitmap, String savedImagePath) {
        if (ctx_yolov5v == 0) {
            return false;
        }
        // ARGB8888 bitmap is only supported in native, other color formats can be added by yourself.
        return nativeProcess_yolov5v(ctx_yolov5v, ARGB8888ImageBitmap, savedImagePath);
    }

    public static native long nativeInit_yolov5v(String modelDir,
                                         String labelPath,
                                         int cpuThreadNum,
                                         String cpuPowerMode,
                                         int inputWidth,
                                         int inputHeight,
                                         float[] inputMean,
                                         float[] inputStd,
                                         float scoreThreshold);

    public static native boolean nativeRelease_yolov5v(long ctx);

    public static native boolean nativeProcess_yolov5v(long ctx, Bitmap ARGB888ImageBitmap, String savedImagePath);

    public static native OcrInfo[] process(long ctx,Bitmap bitmap,byte[] nv21data);

}
