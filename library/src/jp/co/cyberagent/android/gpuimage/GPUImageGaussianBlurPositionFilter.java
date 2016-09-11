/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage;

import android.graphics.PointF;
import android.opengl.GLES20;

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize value ranging from 0.0 on up, with a default of 1.0
 */
public class GPUImageGaussianBlurPositionFilter extends GPUImageTwoPassTextureSamplingFilter {
    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "const int GAUSSIAN_SAMPLES = 9;\n" +
            "\n" +
            "uniform float texelWidthOffset;\n" +
            "uniform float texelHeightOffset;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	gl_Position = position;\n" +
            "	textureCoordinate = inputTextureCoordinate.xy;\n" +
            "	\n" +
//            "	// Calculate the positions for the blur\n" +
            "	int multiplier = 0;\n" +
            "	vec2 blurStep;\n" +
            "   vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset);\n" +
            "    \n" +
            "	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)\n" +
            "   {\n" +
            "		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n" +
//            "       // Blur in x (horizontal)\n" +
            "       blurStep = float(multiplier) * singleStepOffset;\n" +
            "		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
            "	}\n" +
            "}\n";

    public static final String FRAGMENT_SHADER =
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "const lowp int GAUSSIAN_SAMPLES = 9;\n" +
            "\n" +
            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
            "\n" +
            "uniform float aspectRatio;\n" +
            "uniform float blurRadius;\n" +
            "uniform vec2 blurCenter;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec2 textureCoordinateToUse = vec2(textureCoordinate.x, (textureCoordinate.y * aspectRatio + 0.5 - 0.5 * aspectRatio));\n" +
            "    float dist = distance(blurCenter, textureCoordinateToUse);\n" +
            "\n" +
            "    if (dist < blurRadius) {\n" +
            "        vec4 sum = vec4(0.0);\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[0]) * 0.05;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[1]) * 0.09;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[2]) * 0.12;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[3]) * 0.15;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[4]) * 0.18;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[5]) * 0.15;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[6]) * 0.12;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[7]) * 0.09;\n" +
            "        sum += texture2D(inputImageTexture, blurCoordinates[8]) * 0.05;\n" +
            "	     gl_FragColor = sum;\n" +
            "	 } else {\n" +
            "	     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "	 }\n" +
            "}";

    protected float mBlurSize = 1f;
    protected float mAspectRatio = 1.0f;
    protected float mRadius = 1.0f;
    private PointF mCenter;

    private int mAspectRatioLocation;
    private int mCenterLocation;
    private int mRadiusLocation;

    public GPUImageGaussianBlurPositionFilter() {
        this(1.5f, 0.5f, new PointF(0.5f, 0.5f));
    }

    public GPUImageGaussianBlurPositionFilter(float blurSize, float radius, PointF center) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        mBlurSize = blurSize;
        mRadius = radius;
        mCenter = center;
    }

    @Override
    public void onInit() {
        super.onInit();
        GPUImageFilter filter = mFilters.get(1);
        mAspectRatioLocation = GLES20.glGetUniformLocation(filter.getProgram(), "aspectRatio");
        mCenterLocation = GLES20.glGetUniformLocation(filter.getProgram(), "blurCenter");
        mRadiusLocation = GLES20.glGetUniformLocation(filter.getProgram(), "blurRadius");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setCenter(mCenter);
        setRadius(mRadius);
        setAspectRatio(1.0f);
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return mBlurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return mBlurSize;
    }

    /**
     * A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
     *
     * @param blurSize from 0.0 on up, default 1.0
     */
    public void setBlurSize(float blurSize) {
        mBlurSize = blurSize;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    public void setAspectRatio(final float aspectRatio) {
        mAspectRatio = aspectRatio;
        setFloat(mAspectRatioLocation, mAspectRatio);
    }

    public void setCenter(final PointF center) {
        mCenter = center;
        setPoint(mCenterLocation, mCenter);
    }

    public void setRadius(final float radius) {
        mRadius = radius;
        setFloat(mRadiusLocation, mRadius);
    }
}
