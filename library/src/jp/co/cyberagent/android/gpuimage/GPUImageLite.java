package jp.co.cyberagent.android.gpuimage;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

/**
 * Created by Administrator on 2016/9/16.
 */
public class GPUImageLite {
    private final Context mContext;
    private final GPUImageRenderer mRenderer;
    private GPUImageFilter mFilter;
    private Bitmap mCurrentBitmap;
    private GPUImageScaleType mScaleType = GPUImageScaleType.CENTER_CROP;
    private PixelBuffer mPixelBuffer;

    public GPUImageLite(final Context context) {
        if (!supportsOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        mFilter = new GPUImageFilter();
        mRenderer = new GPUImageRenderer(mFilter);
    }

    /**
     * Checks if OpenGL ES 2.0 is supported on the current device.
     *
     * @param context the context
     * @return true, if successful
     */
    private boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    /**
     * Sets the background color
     *
     * @param red red color value
     * @param green green color value
     * @param blue red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        mRenderer.setBackgroundColor(red, green, blue);
    }

    /**
     * Request the preview to be rendered again.
     */
    public void requestRender() {
    }

    /**
     * Sets the filter which should be applied to the image which was (or will
     * be) set by setImage(...).
     *
     * @param filter the new filter
     */
    public void setFilter(final GPUImageFilter filter) {
        if (mFilter == filter) {
            return;
        }
        if (null != mFilter) {
            mFilter.destroy();
        }
        mFilter = filter;
        mRenderer.setFilter(mFilter);
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public void setImage(final Bitmap bitmap) {
        mCurrentBitmap = bitmap;
        mPixelBuffer = new PixelBuffer(mCurrentBitmap.getWidth(), mCurrentBitmap.getHeight());
        mPixelBuffer.setRenderer(mRenderer);
        mRenderer.setImageBitmap(bitmap, false);
        requestRender();
    }

    /**
     * This sets the scale type of GPUImage. This has to be run before setting the image.
     * If image is set and scale type changed, image needs to be reset.
     *
     * @param scaleType The new ScaleType
     */
    public void setScaleType(GPUImageScaleType scaleType) {
        mScaleType = scaleType;
        mRenderer.setScaleType(scaleType);
        mRenderer.deleteImage();
        mCurrentBitmap = null;
        requestRender();
    }

    /**
     * Sets the rotation of the displayed image.
     *
     * @param rotation new rotation
     */
    public void setRotation(Rotation rotation) {
        mRenderer.setRotation(rotation);
    }

    /**
     * Sets the rotation of the displayed image with flip options.
     *
     * @param rotation new rotation
     */
    public void setRotation(Rotation rotation, boolean flipHorizontal, boolean flipVertical) {
        mRenderer.setRotation(rotation, flipHorizontal, flipVertical);
    }

    /**
     * Deletes the current image.
     */
    public void deleteImage() {
        mRenderer.deleteImage();
        if (null != mCurrentBitmap) {
            mCurrentBitmap.recycle();
            mCurrentBitmap = null;
        }
        requestRender();
    }

    public void fillBitmapWithFilterApplied(Bitmap bitmap) {
        if (null != mPixelBuffer) {
            mPixelBuffer.getBitmap(bitmap);
        }
    }

    /**
     * Gets the current displayed image with applied filter as a Bitmap.
     *
     * @return the current image with filter applied
     */
    public Bitmap getBitmapWithFilterApplied() {
        if (null != mPixelBuffer) {
            return mPixelBuffer.getBitmap();
        }
        return null;
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap) {

        GPUImageRenderer renderer = new GPUImageRenderer(mFilter);
        renderer.setRotation(Rotation.NORMAL,
                mRenderer.isFlippedHorizontally(), mRenderer.isFlippedVertically());
        renderer.setScaleType(mScaleType);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, false);
        Bitmap result = buffer.getBitmap();
        mFilter.destroy();
        renderer.deleteImage();
        buffer.destroy();

        mRenderer.setFilter(mFilter);
        if (mCurrentBitmap != null) {
            mRenderer.setImageBitmap(mCurrentBitmap, false);
        }
        requestRender();

        return result;
    }
}
