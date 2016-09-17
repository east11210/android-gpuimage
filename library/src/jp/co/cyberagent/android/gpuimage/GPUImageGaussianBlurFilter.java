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

import android.opengl.GLES20;

public class GPUImageGaussianBlurFilter extends GPUImageTwoPassTextureSamplingFilter {

    public static final String CONTRAST_FRAGMENT_SHADER =
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "\n" +
                "uniform float texelWidthOffset;\n" +
                "uniform float texelHeightOffset;\n" +
                "\n" +
                "varying vec2 blurCoordinate;\n" +
                "varying vec2 blurOffset;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    blurCoordinate = inputTextureCoordinate.xy;\n" +
                "    blurOffset = vec2(texelHeightOffset, texelWidthOffset);\n" +
                "}\n";

    private static String fragmentShader(final int maxRadius) {
        if (1 > maxRadius) {
            return NO_FILTER_FRAGMENT_SHADER;
        }
        final int maxSamples = maxRadius * 2 + 1;
        final String shaderString =
                "const int SAMPLES = " + maxSamples + ";\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform int blurSample;\n" +
                "uniform float blurSteps[SAMPLES];\n" +
                "uniform float blurWeights[SAMPLES];\n" +
                "varying vec2 blurCoordinate;\n" +
                "varying vec2 blurOffset;\n" +
                "void main() {\n" +
                "    lowp vec4 sum = vec4(0.0);\n" +
                "    for (int i = 0; i < blurSample; ++i) {\n" +
                "        lowp vec2 coordinate = blurCoordinate + blurOffset * blurSteps[i];\n" +
                "        sum += texture2D(inputImageTexture, coordinate) * blurWeights[i];\n" +
                "    }\n" +
                "    gl_FragColor = sum;\n" +
                "}\n";
        return shaderString;
    }

    private void prepareArrays(final int radius, final float sigma) {
        int i;
        final int samples = radius * 2 + 1;
        final int weights = radius + 1;
        float sumOfWeights = 0.0f;
//        mWeights = new float[samples];
        for (i = 0; i < weights; ++i) {
            final float weight = (float) ((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(i, 2.0) / (2.0 * Math.pow(sigma, 2.0))));
            if (0 == i) {
                sumOfWeights += weight;
            } else {
                sumOfWeights += 2.0 * weight;
            }
            mWeights[weights -1 -i] = weight;
        }
        for (i = 0; i < weights; ++i) {
            mWeights[i] = mWeights[i] / sumOfWeights;
        }
        for (i = 0; i < radius; ++i) {
            mWeights[samples - i - 1] = mWeights[i];
        }

//        mOffsets = new float[samples];
        int offset = -radius;
        for (i = 0; i < samples; ++i) {
            mOffsets[i] = offset;
            ++offset;
        }
        for (; i < mMaxSamples; ++i) {
            mWeights[i] = 0;
        }
    }

    protected float mBlurSize = 1f;
    private int mMaxSamples;
    private int mMaxRadiusInPixel;
    private int mRadiusInPixel; // sigma
    private int mRadius;
    private float mWeights[];
    private float mOffsets[];
    private int mWeightsLocation[];
    private int mOffsetsLocation[];
    private int mSampleLocation[];

    public GPUImageGaussianBlurFilter() {
        this(getRadius(12), 2);
    }

    public GPUImageGaussianBlurFilter(final int maxRadiusInPiexel, final int radiusInPixel) {
        this(1.0f, radiusInPixel, maxRadiusInPiexel, getRadius(maxRadiusInPiexel));
    }

    public GPUImageGaussianBlurFilter(float blurSize, int radiusInPixel,
                                      int maxRadiusInPiexel, int maxRadius) {
        super(CONTRAST_FRAGMENT_SHADER, fragmentShader(maxRadius),
                CONTRAST_FRAGMENT_SHADER, fragmentShader(maxRadius));
        mBlurSize = blurSize;
        mMaxSamples = maxRadius * 2 + 1;
        mMaxRadiusInPixel = maxRadiusInPiexel;
        mRadiusInPixel = radiusInPixel;
        mRadius = getRadius(radiusInPixel);
        mOffsets = new float[mMaxSamples];
        mWeights = new float[mMaxSamples];
        mWeightsLocation = new int[2];
        mOffsetsLocation = new int[2];
        mSampleLocation = new int[2];
        prepareArrays(mRadius, mRadiusInPixel);
    }

//    @Override
//    public void onInit() {
//        super.onInit();
//    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        initLocaltion(0);
        initLocaltion(1);
        setUniforms(0);
        setUniforms(1);
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

    private void initLocaltion(int index) {
        GPUImageFilter filter = mFilters.get(index);
        mOffsetsLocation[index] = GLES20.glGetUniformLocation(filter.getProgram(), "blurSteps");
        mWeightsLocation[index] = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
        mSampleLocation[index] = GLES20.glGetUniformLocation(filter.getProgram(), "blurSample");
    }

    public void setUniforms(int index) {
        GPUImageFilter filter = mFilters.get(index);
        filter.setFloatArray(mOffsetsLocation[index], mOffsets);
        filter.setFloatArray(mWeightsLocation[index], mWeights);
        filter.setInteger(mSampleLocation[index], mRadius * 2 + 1);
    }

    private static int getRadius(final int radiusInPixel) {
        final float minimumWeightToFindEdgeOfSamplingArea = 1.0f/256.0f;
        return (int) Math.floor(Math.sqrt(
                -2.0 * Math.pow(radiusInPixel, 2.0)
                        * Math.log(minimumWeightToFindEdgeOfSamplingArea * Math.sqrt(2.0 * Math.PI * Math.pow(radiusInPixel, 2.0)))
        ));
    }

    public void setRadiusInPixel(final int radiusInPixel) {
        if (mRadiusInPixel == radiusInPixel
                || 1 > radiusInPixel
                || mMaxRadiusInPixel < radiusInPixel) {
            return;
        }
        mRadiusInPixel = radiusInPixel;
        mRadius = getRadius(mRadiusInPixel);
        prepareArrays(mRadius, mRadiusInPixel);

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                setUniforms(0);
                setUniforms(1);
            }
        });
    }
}
