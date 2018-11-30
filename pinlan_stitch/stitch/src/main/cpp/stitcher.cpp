#include <jni.h>
#include <vector>
#include <android/log.h>

#include "opencv2/opencv.hpp"

using namespace cv;
using namespace std;

#define  LOG_TAG    "pinshi_stitch"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C"
{
JNIEXPORT jint JNICALL
Java_com_example_stitch_StitchActivity_StitchPanorama(JNIEnv *env, jobject,
                                                                   jlongArray imageAddressArray,
                                                                   jlong outputAddress) {
    jsize a_len = env->GetArrayLength(imageAddressArray);
    jlong *imgAddressArr = env->GetLongArrayElements(imageAddressArray,0);
    vector<Mat> imgVec;
    for(int	k=0; k<a_len; k++)
    {
        Mat	&curimage = *(Mat*)imgAddressArr[k];
        imgVec.push_back(curimage);
    }
    Mat	&result = *(Mat*)outputAddress;
    Stitcher stitcher = Stitcher::createDefault(false);
    Stitcher::Status status = stitcher.stitch(imgVec, result);

    LOGD("Result height %d width %d", result.rows, result.cols);

    return status;
}
}
