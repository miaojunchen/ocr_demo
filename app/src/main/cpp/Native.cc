// Copyright (c) 2019 PaddlePaddle Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "Native.h"
#include "pipeline.h"
#include "unify.h"
#include <android/log.h>
#include <android/bitmap.h>

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_baidu_paddle_lite_demo_ocr_db_crnn_Native
 * Method:    nativeInit
 * Signature:
 * (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_Native_nativeInit(
        JNIEnv *env, jclass thiz, jstring jDetModelPath, jstring jClsModelPath,
        jstring jRecModelPath, jstring jConfigPath, jstring jLabelPath,
        jint cpuThreadNum, jstring jCPUPowerMode) {
  std::string detModelPath = jstring_to_cpp_string(env, jDetModelPath);
  std::string clsModelPath = jstring_to_cpp_string(env, jClsModelPath);
  std::string recModelPath = jstring_to_cpp_string(env, jRecModelPath);
  std::string configPath = jstring_to_cpp_string(env, jConfigPath);
  std::string labelPath = jstring_to_cpp_string(env, jLabelPath);
  std::string cpuPowerMode = jstring_to_cpp_string(env, jCPUPowerMode);

  return reinterpret_cast<jlong>(
          new Pipeline(detModelPath, clsModelPath, recModelPath, cpuPowerMode,
                       cpuThreadNum, configPath, labelPath));
}

/*
 * Class:     com_baidu_paddle_lite_demo_ocr_db_crnn_Native
 * Method:    nativeRelease
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_Native_nativeRelease(JNIEnv *env,
                                                                 jclass thiz,
                                                                 jlong ctx) {
  if (ctx == 0) {
    return JNI_FALSE;
  }
  Pipeline *pipeline = reinterpret_cast<Pipeline *>(ctx);
  delete pipeline;
  return JNI_TRUE;
}

/*
 * Class:     com_baidu_paddle_lite_demo_ocr_db_crnn_Native
 * Method:    nativeProcess
 * Signature: (JIIIILjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_Native_nativeProcess(
        JNIEnv *env, jclass thiz, jlong ctx, jint inTextureId, jint outTextureId,
        jint textureWidth, jint textureHeight, jstring jsavedImagePath) {
  if (ctx == 0) {
    return JNI_FALSE;
  }
  std::string savedImagePath = jstring_to_cpp_string(env, jsavedImagePath);
  Pipeline *pipeline = reinterpret_cast<Pipeline *>(ctx);
  return pipeline->Process_val(inTextureId, outTextureId, textureWidth,
                               textureHeight, savedImagePath);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_Native_process(JNIEnv *env, jclass clazz, jlong ctx,
                                                           jobject jARGB8888ImageBitmap,jbyteArray arraynv21) {

  if (ctx == 0) {
    return JNI_FALSE;
  }

  // Convert the android bitmap(ARGB8888) to the OpenCV RGBA image. Actually,
  // the data layout of AGRB8888 is R, G, B, A, it's the same as CV RGBA image,
  // so it is unnecessary to do the conversion of color format, check
  // https://developer.android.com/reference/android/graphics/Bitmap.Config#ARGB_8888
  // to get the more details about Bitmap.Config.ARGB8888
//  auto t = GetCurrentTime();
//  void *bitmapPixels;
//  AndroidBitmapInfo bitmapInfo;
//  if (AndroidBitmap_getInfo(env, jARGB8888ImageBitmap, &bitmapInfo) < 0) {
//    LOGE("Invoke AndroidBitmap_getInfo() failed!");
//    return JNI_FALSE;
//  }
//  if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
//    LOGE("Only Bitmap.Config.ARGB8888 color format is supported!");
//    return JNI_FALSE;
//  }
//  if (AndroidBitmap_lockPixels(env, jARGB8888ImageBitmap, &bitmapPixels) < 0) {
//    LOGE("Invoke AndroidBitmap_lockPixels() failed!");
//    return JNI_FALSE;
//  }
//  cv::Mat bmpImage(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
//  cv::Mat rgbaImage;
//  bmpImage.copyTo(rgbaImage);
//  if (AndroidBitmap_unlockPixels(env, jARGB8888ImageBitmap) < 0) {
//    LOGE("Invoke AndroidBitmap_unlockPixels() failed!");
//    return JNI_FALSE;
//  }
//  LOGD("Read from bitmap costs %f ms", GetElapsedTime(t));

  int len = env->GetArrayLength(arraynv21);
  uint8_t* buf = new uint8_t[len];
  env->GetByteArrayRegion(arraynv21, 0,len,reinterpret_cast<jbyte*>(buf));
  cv::Mat nv21(cv::Size(1920, 1080 + 1080 / 2), CV_8UC1, buf);
  cv::Mat rgbaImage;
  cv::cvtColor(nv21, rgbaImage, cv::COLOR_YUV2RGBA_NV21);
//  env->ReleaseByteArrayElements(arraynv21, c_data_nv21, 0);
  auto pipeline = reinterpret_cast<Pipeline *>(ctx);
  std::vector<OCR_INFO> result = pipeline->Process_val(rgbaImage);
    jclass ocrInfo_jclass = env->FindClass("com/baidu/paddle/lite/demo/OcrInfo");
  jclass point_class = env->FindClass("android/graphics/Point");

  jobjectArray array = env->NewObjectArray(result.size(),ocrInfo_jclass,nullptr);

  for (int i= 0;i< result.size();i++){
    jmethodID  jmethodId = env->GetMethodID(ocrInfo_jclass, "<init>", "()V");
    jmethodID  poinInstance = env->GetMethodID(point_class, "<init>", "(II)V");
    jobject  ocrInfo_instance = env->NewObject(ocrInfo_jclass,jmethodId);

    auto corInfo = result[i];

    jfieldID  name = env->GetFieldID(ocrInfo_jclass,"text","Ljava/lang/String;");
    jfieldID  score = env->GetFieldID(ocrInfo_jclass,"score","F");
    jfieldID point_file_id = env->GetFieldID(ocrInfo_jclass,"point","[Landroid/graphics/Point;");

    auto name_str = env->NewStringUTF(corInfo.text.c_str());
    env->SetObjectField(ocrInfo_instance,name,name_str);

    env->SetFloatField(ocrInfo_instance,score, corInfo.score);
    auto pts = corInfo.pts;
    jobjectArray  pointArray = env->NewObjectArray(4,point_class, nullptr);

    for (int i = 0;i < 4;i++) {
        jobject  point  = env->NewObject(point_class,poinInstance,pts[i].x,pts[i].y);

        env->SetObjectArrayElement(pointArray,i,point);
    }

    env->SetObjectField(ocrInfo_instance,point_file_id, pointArray);
    env->SetObjectArrayElement(array,i,ocrInfo_instance);
  }
  delete buf;
  return array;
}


#ifdef __cplusplus
}
#endif
extern "C"
JNIEXPORT jlong JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_UnityNative_creat(JNIEnv *env, jobject thiz) {
  return reinterpret_cast<jlong>(
          new RecDet());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_UnityNative_init_1deet(JNIEnv *env, jobject thiz,
                                                                  jlong ctx, jstring model_dir,
                                                                  jstring label_path,
                                                                  jint cpu_thread_num,
                                                                  jstring cpu_power_mode,
                                                                  jint input_width,
                                                                  jint input_height,
                                                                  jfloatArray input_mean,
                                                                  jfloatArray input_std,
                                                                  jfloat score_threshold) {
  if (ctx == 0) {
    return;
  }
  RecDet *pipeline = reinterpret_cast<RecDet *>(ctx);
  std::string modelDir = jstring_to_cpp_string(env, model_dir);
  std::string labelPath = jstring_to_cpp_string(env, label_path);
  std::string cpuPowerMode = jstring_to_cpp_string(env, cpu_power_mode);
  std::vector<float> inputMean = jfloatarray_to_float_vector(env, input_mean);
  std::vector<float> inputStd = jfloatarray_to_float_vector(env, input_std);
  pipeline->init_det(modelDir, labelPath, cpu_thread_num, cpuPowerMode, input_width,
                               input_height, inputMean, inputStd, score_threshold);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_UnityNative_init_1ocr(JNIEnv *env, jobject thiz,
                                                                  jlong ctx, jstring det_model_path,
                                                                  jstring cls_model_path,
                                                                  jstring rec_model_path,
                                                                  jstring config_path,
                                                                  jstring label_path,
                                                                  jint cput_thread_num,
                                                                  jstring cpu_power_mode) {
  if (ctx == 0) {
    return;
  }
  RecDet *pipeline = reinterpret_cast<RecDet *>(ctx);
  std::string detModelPath = jstring_to_cpp_string(env, det_model_path);
  std::string clsModelPath = jstring_to_cpp_string(env, cls_model_path);
  std::string recModelPath = jstring_to_cpp_string(env, rec_model_path);
  std::string configPath = jstring_to_cpp_string(env, config_path);
  std::string labelPath = jstring_to_cpp_string(env, label_path);
  std::string cpuPowerMode = jstring_to_cpp_string(env, cpu_power_mode);
  pipeline->init_ocr(detModelPath, clsModelPath, recModelPath, cpuPowerMode,
                     cput_thread_num, configPath, labelPath);
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_baidu_paddle_lite_demo_ppocr_1demo_UnityNative_process(JNIEnv *env, jclass clazz,
                                                                jlong ctx, jobject jARGB8888ImageBitmap) {
    LOGE("Invoke AndroidBitmap_getInfo()ddd failed %d!", ctx);

  if (ctx == 0) {
    return JNI_FALSE;
  }
  LOGE("Invoke AndroidBitmap_getInfo()ddd failed!");
  void *bitmapPixels;
  AndroidBitmapInfo bitmapInfo;
  if (AndroidBitmap_getInfo(env, jARGB8888ImageBitmap, &bitmapInfo) < 0) {
    LOGE("Invoke AndroidBitmap_getInfo() failed!");
    return JNI_FALSE;
  }
  LOGE("Invoke AndroidBitmap_getInfo()ddd failed!");
  if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    LOGE("Only Bitmap.Config.ARGB8888 color format is supported!");
    return JNI_FALSE;
  }
  if (AndroidBitmap_lockPixels(env, jARGB8888ImageBitmap, &bitmapPixels) < 0) {
    LOGE("Invoke AndroidBitmap_lockPixels() failed!");
    return JNI_FALSE;
  }
    LOGE("Invoke AndroidBitmap_getInfo()ddd dfdfailed!");

  cv::Mat bmpImage(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
  cv::Mat rgbaImage;
  bmpImage.copyTo(rgbaImage);
  if (AndroidBitmap_unlockPixels(env, jARGB8888ImageBitmap) < 0) {
    LOGE("Invoke AndroidBitmap_unlockPixels() failed!");
    return JNI_FALSE;
  }
  LOGE("Invoke AndroidBitmap_getInfo() fadddfdafdasfiled, %d, %d!", rgbaImage.rows, rgbaImage.cols);
  RecDet *pipeline = reinterpret_cast<RecDet *>(ctx);
    LOGE("Invoke AndroidBitmap_getInfo() fdafdasfdsfdsgsa!");
  ALL_RET allres = pipeline->process(rgbaImage);
    LOGE("Invoke AndroidBitmap_getInfo() fadddiled!");
  jclass allRet_jclass = env->FindClass("com/baidu/paddle/lite/demo/AllRet");

  jclass obj_jclass = env->FindClass("com/baidu/paddle/lite/demo/Obj");
  jclass rect_jclass = env->FindClass("android/graphics/Rect");

  jclass ocrInfo_jclass = env->FindClass("com/baidu/paddle/lite/demo/OcrInfo");
  jclass point_class = env->FindClass("android/graphics/Point");

  jobjectArray ocrs_array = env->NewObjectArray(allres.ocrs.size(),ocrInfo_jclass,nullptr);
  jobjectArray dets_array = env->NewObjectArray(allres.dets.size(),obj_jclass,nullptr);

  //cvt ocrs
  for (int i= 0;i< allres.ocrs.size();i++){
    jmethodID  jmethodId = env->GetMethodID(ocrInfo_jclass, "<init>", "()V");
    jmethodID  poinInstance = env->GetMethodID(point_class, "<init>", "(II)V");
    jobject  ocrInfo_instance = env->NewObject(ocrInfo_jclass,jmethodId);

    auto corInfo = allres.ocrs[i];

    jfieldID  name = env->GetFieldID(ocrInfo_jclass,"text","Ljava/lang/String;");
    jfieldID  score = env->GetFieldID(ocrInfo_jclass,"score","F");
    jfieldID point_file_id = env->GetFieldID(ocrInfo_jclass,"point","[Landroid/graphics/Point;");

    auto name_str = env->NewStringUTF(corInfo.text.c_str());
    env->SetObjectField(ocrInfo_instance,name,name_str);

    env->SetFloatField(ocrInfo_instance,score, corInfo.score);
    auto pts = corInfo.pts;
    jobjectArray  pointArray = env->NewObjectArray(4,point_class, nullptr);

    for (int i = 0;i < 4;i++) {
      jobject  point  = env->NewObject(point_class,poinInstance,pts[i].x,pts[i].y);

      env->SetObjectArrayElement(pointArray,i,point);
    }

    env->SetObjectField(ocrInfo_instance,point_file_id, pointArray);
    env->SetObjectArrayElement(ocrs_array,i,ocrInfo_instance);
  }

  //cvt dets
  for (int i = 0; i < allres.dets.size(); ++i) {
    auto det = allres.dets[i];

    jmethodID  jmethodId = env->GetMethodID(obj_jclass, "<init>", "()V");
    jmethodID  rectId = env->GetMethodID(rect_jclass, "<init>", "(IIII)V");
    jobject  det_instance = env->NewObject(obj_jclass,jmethodId);

    jfieldID  class_name = env->GetFieldID(obj_jclass,"class_name","Ljava/lang/String;");
    jfieldID  prob = env->GetFieldID(obj_jclass,"prob","F");

    auto name_str = env->NewStringUTF(det.class_name.c_str());
    env->SetObjectField(det_instance,class_name,name_str);

    env->SetFloatField(det_instance,prob, det.prob);

    jobject rectInstance = env->NewObject(rect_jclass, rectId, det.rec.x,det.rec.y,det.rec.x + det.rec.width,det.rec.y+det.rec.height);
    jfieldID rect_file_id = env->GetFieldID(obj_jclass,"rec", "Landroid/graphics/Rect;");
    env->SetObjectField(det_instance,rect_file_id, rectInstance);

    jfieldID  cls_id = env->GetFieldID(obj_jclass,"class_id","I");
    env->SetIntField(det_instance,cls_id, det.class_id);
    env->SetObjectArrayElement(dets_array,i,det_instance);
  }

  jfieldID oid = env->GetFieldID(allRet_jclass,"ocrs","[Lcom/baidu/paddle/lite/demo/OcrInfo;");
  jfieldID did = env->GetFieldID(allRet_jclass,"dets","[Lcom/baidu/paddle/lite/demo/Obj;");
  jmethodID  allRetID = env->GetMethodID(allRet_jclass, "<init>", "()V");
  jobject  allRetInstance = env->NewObject(allRet_jclass,allRetID);
  env->SetObjectField(allRetInstance,oid,ocrs_array);
  env->SetObjectField(allRetInstance,did,dets_array);
    LOGE("Invoke AndroidBitmap_getInfo() failedddd %p!", allRetInstance);
  return allRetInstance;
}