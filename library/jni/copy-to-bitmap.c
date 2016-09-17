#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <GLES/GL.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libgpuimage", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libgpuimage", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libgpuimage", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libgpuimage", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libgpuimage", __VA_ARGS__)

// Image in Bitmap object will be upside down

JNIEXPORT jboolean JNICALL Java_jp_co_cyberagent_android_gpuimage_GPUImageNativeLibrary_CopyToBitmap(JNIEnv * env, jclass clazz, jobject bitmap)
{
    AndroidBitmapInfo   BitmapInfo;
    void *              pPixels;
    int                 ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &BitmapInfo)) < 0)
    {
        LOGE("Error - AndroidBitmap_getInfo() Failed! error: %d", ret);
        return JNI_FALSE;
    }

    if (BitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        LOGE("Error - Bitmap format is not RGBA_8888!");
        return JNI_FALSE;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pPixels)) < 0)
    {
        LOGE("Error - AndroidBitmap_lockPixels() Failed! error: %d", ret);
        return JNI_FALSE;
    }

    glReadPixels(0, 0, BitmapInfo.width, BitmapInfo.height, GL_RGBA, GL_UNSIGNED_BYTE, pPixels);

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}
