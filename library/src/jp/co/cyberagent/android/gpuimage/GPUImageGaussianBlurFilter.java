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

    private static String fragmentShader(final int radius) {
        if (1 > radius) {
            return NO_FILTER_FRAGMENT_SHADER;
        }
        final int samples = radius * 2 + 1;
        final String shaderString =
                "const int SAMPLES = " + samples + ";\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform float blurSteps[SAMPLES];\n" +
                "uniform float blurWeights[SAMPLES];\n" +
                "varying vec2 blurCoordinate;\n" +
                "varying vec2 blurOffset;\n" +
                "void main() {\n" +
                "    lowp vec4 sum = vec4(0.0);\n" +
                "    for (int i = 0; i < SAMPLES; ++i) {\n" +
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
        mWeights = new float[samples];
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

        mOffsets = new float[samples];
        int offset = -radius;
        for (i = 0; i < samples; ++i) {
            mOffsets[i] = offset;
            ++offset;
        }
    }

    protected float mBlurSize = 1f;
    protected int mRadiusInPixel = 2;
    private float mWeights[];
    private float mOffsets[];

    public GPUImageGaussianBlurFilter() {
        this(1.0f, 2, getRadius(2));
    }

    public GPUImageGaussianBlurFilter(float blurSize, int radiusInPixel, int radius) {
        super(CONTRAST_FRAGMENT_SHADER, fragmentShader(radius),
                CONTRAST_FRAGMENT_SHADER, fragmentShader(radius));
        mBlurSize = blurSize;
        mRadiusInPixel = radiusInPixel;
        prepareArrays(radius, radiusInPixel);
    }

//    @Override
//    public void onInit() {
//        super.onInit();
//    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setArrays();
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

    public void setArrays() {
        GPUImageFilter filter = mFilters.get(0);
        int location = GLES20.glGetUniformLocation(filter.getProgram(), "blurSteps");
        filter.setFloatArray(location, mOffsets);
        location = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
        filter.setFloatArray(location, mWeights);
        filter = mFilters.get(1);
        location = GLES20.glGetUniformLocation(filter.getProgram(), "blurSteps");
        filter.setFloatArray(location, mOffsets);
        location = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
        filter.setFloatArray(location, mWeights);
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
                || 1 > radiusInPixel) {
            return;
        }
        mRadiusInPixel = radiusInPixel;

        runOnDraw(new Runnable() {
            @Override
            public void run() {
                destroyFilters();
                int sampleRadius = getRadius(mRadiusInPixel);
                String fragmentShader = fragmentShader(sampleRadius);
                addFilter(new GPUImageFilter(CONTRAST_FRAGMENT_SHADER, fragmentShader));
                addFilter(new GPUImageFilter(CONTRAST_FRAGMENT_SHADER, fragmentShader));
                initFilters();
                initTexelOffsets();
                prepareArrays(sampleRadius, radiusInPixel);
                setArrays();
            }
        });
    }
}
