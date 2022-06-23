//
// Created by 陈妙俊 on 2022/6/22.
//

#include "unify.h"

RecDet::RecDet() : thread_pool_(new ThreadPool(std::max(2U, std::thread::hardware_concurrency())))
{

}

RecDet::~RecDet()
{

}

void RecDet::init_ocr(const std::string &detModelDir, const std::string &clsModelDir,
              const std::string &recModelDir, const std::string &cPUPowerMode,
              const int cPUThreadNum, const std::string &config_path,
              const std::string &dict_path)
{
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::RecDet::init_ocr!");
    ocr_.reset(new Pipeline(detModelDir, clsModelDir, recModelDir, cPUPowerMode,
                            cPUThreadNum, config_path, dict_path));
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::RecDet::ocr_ %p!", ocr_.get());
}

void RecDet::init_det(const std::string &modelDir, const std::string &labelPath,
              const int cpuThreadNum, const std::string &cpuPowerMode,
              int inputWidth, int inputHeight, const std::vector<float> &inputMean,
              const std::vector<float> &inputStd, float scoreThreshold)
{
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::RecDet::init_det!");
    det_.reset(new Yolov5V_Pipeline(modelDir, labelPath, cpuThreadNum, cpuPowerMode, inputWidth,
                                    inputHeight, inputMean, inputStd, scoreThreshold));
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::RecDet::det_ %p!", det_.get());
}

ALL_RET RecDet::process(cv::Mat &rgbaImage)
{

    auto task_ocr = [&] (cv::Mat &rgbaImage) {
        return this->ocr_->Process_val(rgbaImage);
    };
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process!");
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process1 det_ %p!", det_.get());
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process1 ocr_ %p!", ocr_.get());
    //std::future<std::vector<OCR_INFO>> ocr_res = thread_pool_->enqueue(task_ocr, rgbaImage);
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process1!");
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process1!");
//    auto detImg = rgbaImage.clone();
    auto dets = det_->Process(rgbaImage);
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process2!%d", dets.size());
    //auto ocr_final_res = ocr_->Process_val(rgbaImage);
    //auto ocr_final_res = ocr_res.get();
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process3!");
    ALL_RET ret;
    ret.dets = dets;
   // ret.ocrs = ocr_final_res;
    LOGE("Invoke AndroidBitmap_getInfo() RecDet::process4!");
    return ret;
}
