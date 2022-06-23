#ifndef CV_TEXT_H
#define CV_TEXT_H

#include <opencv2/opencv.hpp>

#include <ft2build.h>
#include FT_FREETYPE_H

namespace Utils {
    class CvText {
    public:

        /**
         * 文本编码
         */
        typedef enum {
            UTF8, GB2312
        } TextEncoding;

        CvText(){};
        /**
         * 构造函数，初始化一个字体
         * @param fontName 字体名称
         * @param encoding putText函数的参数中的文本编码，
         * 需要和代码的编码一致，否则会出现乱码。
         * 目前仅支持UTF-8(很多IDE的源码默认编码)和GB2312(
         * Visual Studio中文环境下的默认编码)这两种编码。
         */
        CvText(const char *fontName, TextEncoding encoding = TextEncoding::UTF8);//explicit

        virtual ~CvText();

        /**
         * 设置文本属性
         * @param fontSize 字体大小
         * @param spaceRatio 空格大小比例
         * @param separatorRatio 分隔符大小比例
         * @param fontDiaphaneity 透明度
         */
        void setTextStyle(int fontSize, float spaceRatio = 0.5f,
                          float separatorRatio = 0.1f, float fontDiaphaneity = 1.0f);

        /**
         * 恢复默认文本设置
         */
        void resetTextStyle();

        /**
         * 将text的内容放到frame的指定位置(pos)，默认文本颜色为黑色。
         * @param frame 输出的影象
         * @param text 文本内容
         * @param pos 文本位置
         * @param color 文本颜色
         * @return 返回成功输出的字符长度，失败返回-1。
         */
        int putText(cv::Mat &frame, std::string text, cv::Point pos,
                    cv::Scalar color = cv::Scalar(0, 0, 0));

        /**
          * 将text的内容放到frame的指定位置(pos)，默认颜色为黑色。
          * @param frame 输出的影象
          * @param text 文本内容
          * @param pos 文本位置
          * @param color 文本颜色
          * @return 返回成功输出的字符长度，失败返回-1。
          */
        int putText(cv::Mat &frame, const char *text, cv::Point pos,
                    cv::Scalar color = cv::Scalar(0, 0, 0));

        //私有函数区
    private:
        /**
         * 输出wc到frame的pos位置
         * @param frame 输出Mat
         * @param wc 字符
         * @param pos 位置
         * @param color 颜色
         */
        void putWChar(cv::Mat &frame, wchar_t wc, cv::Point &pos, cv::Scalar color);

        /**
         * 将char字符数组转换为wchar_t字符数组
         * @param src char字符数组
         * @param dst wchar_t字符数组
         * @param locale 语言环境，mbstowcs函数依赖此值来判断src的编码方式
         * @return 运行成功返回0,否则返回-1
         */
        int char2Wchar(const char *&src, wchar_t *&dst, const char *locale = "");

        //私有变量区
    private:
        FT_Library m_library;           // 字库
        FT_Face m_face;                 // 字体

        // 默认的字体输出参数
        int m_fontSize;                 // 字体大小
        float m_spaceRatio;             // 空白字符大小比例
        float m_separatorRatio;         // 字符间分隔距离大小比例
        float m_fontDiaphaneity;        // 透明度
        std::string m_textEncoding;     // 文本编码
    };
}

#endif // CV_TEXT_H