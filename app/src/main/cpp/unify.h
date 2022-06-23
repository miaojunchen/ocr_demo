//
// Created by 陈妙俊 on 2022/6/22.
//

#ifndef PPOCR_DEMO_UNIFY_H
#define PPOCR_DEMO_UNIFY_H
#include "pipeline.h"
#include "yolov5v/Yolov5V_Pipeline.h"
#include "ThreadPool.h"
struct ALL_RET
{
    std::vector<Object> dets;
    std::vector<OCR_INFO> ocrs;
};
class RecDet
{
public:
    RecDet();
    ~RecDet();
    void init_ocr(const std::string &detModelDir, const std::string &clsModelDir,
                  const std::string &recModelDir, const std::string &cPUPowerMode,
                  const int cPUThreadNum, const std::string &config_path,
                  const std::string &dict_path);
    void init_det(const std::string &modelDir, const std::string &labelPath,
                  const int cpuThreadNum, const std::string &cpuPowerMode,
                  int inputWidth, int inputHeight, const std::vector<float> &inputMean,
                  const std::vector<float> &inputStd, float scoreThreshold);
    ALL_RET process(cv::Mat &rgbaImage);

private:
    std::shared_ptr<Yolov5V_Pipeline> det_;
    std::shared_ptr<Pipeline> ocr_;
    std::unique_ptr<ThreadPool> thread_pool_;
};
#endif //PPOCR_DEMO_UNIFY_H
