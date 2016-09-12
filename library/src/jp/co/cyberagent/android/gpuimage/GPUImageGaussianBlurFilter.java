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

public class GPUImageGaussianBlurFilter extends GPUImageTwoPassTextureSamplingFilter {

    private static String vertexShaderForStandardBlurOfRadius(final int radius, final float sigma) {
        if (1 > radius) {
            return NO_FILTER_VERTEX_SHADER;
        }
        int samples = radius * 2 + 1;
        String shaderString =
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "\n" +
                "const int SAMPLES = " + samples + ";\n" +
                "uniform float texelWidthOffset;\n" +
                "uniform float texelHeightOffset;\n" +
                "\n" +
                "varying vec2 blurCoordinates[SAMPLES];\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "	gl_Position = position;\n" +
                "	\n" +
//            "	// Calculate the positions for the blur\n" +
                "	int multiplier = 0;\n" +
                "   vec2 blurStep;\n" +
                "   vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset);\n" +
                "    \n" +
                "	for (int i = 0; i < SAMPLES; i++)\n" +
                "   {\n" +
                "		multiplier = (i - ((SAMPLES - 1) / 2));\n" +
//            "       // Blur in x (horizontal)\n" +
                "       blurStep = float(multiplier) * singleStepOffset;\n" +
                "		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
                "	}\n" +
                "}\n";
        return shaderString;
    }

    private static String fragmentShaderForStandardBlurOfRadius(final int radius, final float sigma) {
        if (1 > radius) {
            return NO_FILTER_FRAGMENT_SHADER;
        }
        // First, generate the normal Gaussian weights for a given sigma
        final int samples = radius * 2+ 1;
        String shaderString =
                "uniform sampler2D inputImageTexture;\n" +
                "const int SAMPLES = " + samples + ";\n" +
                "varying vec2 blurCoordinates[SAMPLES];\n" +
                "uniform float blurWeights[" + (samples) +  "];\n" +
                "void main() {\n" +
                "    lowp vec4 sum = vec4(0.0);\n" +
                "    for (int i = 0; i < SAMPLES; ++i) {\n" +
                "        sum += texture2D(inputImageTexture, blurCoordinates[i]) * blurWeights[i];\n" +
                "    }\n" +
                "    gl_FragColor = sum;\n" +
                "}\n";
//        for (i = 0; i < samples; ++i) {
//            int offsetFromCenter = i - radius;
//            shaderString += "    sum += texture2D(inputImageTexture, blurCoordinates[" + i + "]) * ";
//            if (0 > offsetFromCenter) {
//                shaderString += standardGaussianWeights[-offsetFromCenter];
//            } else {
//                shaderString += standardGaussianWeights[ offsetFromCenter];
//            }
//            shaderString += ";\n";
//        }
//        shaderString +=
//                "    gl_FragColor = sum;\n" +
//                "}\n";
        return shaderString;
    }

    private static float[] fragmentWeightsForStandardBlurOfRadius(final int radius, final float sigma) {
        int i;
        final int samples = radius * 2+ 1;
        final int weights = radius + 1;
        float sumOfWeights = 0.0f;
        float standardGaussianWeights[] = new float[weights];
        for (i = 0; i < weights; ++i) {
            standardGaussianWeights[i] = (float) ((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(i, 2.0) / (2.0 * Math.pow(sigma, 2.0))));
            if (0 == i) {
                sumOfWeights += standardGaussianWeights[i];
            } else {
                sumOfWeights += 2.0 * standardGaussianWeights[i];
            }
        }
        for (i = 0; i < weights; ++i) {
            standardGaussianWeights[i] = standardGaussianWeights[i] / sumOfWeights;
        }
        float result[] = new float[samples];
        for (i = 0; i < samples; ++i) {
            int id = i - radius;
            result[i] = standardGaussianWeights[0 > id ? -id : id];
        }
        return result;
    }

    protected float mBlurSize = 1f;
    protected int mRadiusInPixel = 2;
    private float mWeights[];
    private int mWeightsLocation0;
    private int mWeightsLocation1;

    public GPUImageGaussianBlurFilter() {
        this(1.0f, 2);
    }

    public GPUImageGaussianBlurFilter(float blurSize, int radiusInPixel) {
        super(vertexShaderForStandardBlurOfRadius(4, 2.0f),
            fragmentShaderForStandardBlurOfRadius(4, 2.0f),
            vertexShaderForStandardBlurOfRadius(4, 2.0f),
            fragmentShaderForStandardBlurOfRadius(4, 2.0f));
        mBlurSize = blurSize;
        mRadiusInPixel = radiusInPixel;
        mWeights = fragmentWeightsForStandardBlurOfRadius(4, 2.0f);
    }

    @Override
    public void onInit() {
        super.onInit();
        GPUImageFilter filter = mFilters.get(0);
        mWeightsLocation0 = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
        filter = mFilters.get(1);
        mWeightsLocation1 = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setWeights(mWeights);
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

    public void setWeights(final float weights[]) {
        mWeights = weights;
        GPUImageFilter filter = mFilters.get(0);
        filter.setFloatArray(mWeightsLocation0, mWeights);
        filter = mFilters.get(1);
        filter.setFloatArray(mWeightsLocation1, mWeights);
    }

//    public void setAspectRatio(final float aspectRatio) {
//        mAspectRatio = aspectRatio;
//        setFloat(mAspectRatioLocation, mAspectRatio);
//    }
//
//    public void setCenter(final PointF center) {
//        mCenter = center;
//        setPoint(mCenterLocation, mCenter);
//    }
//
//    public void setRadius(final float radius) {
//        mRadius = radius;
//        setFloat(mRadiusLocation, mRadius);
//    }

    public void setRadiusInPixel(final int radiusInPixel) {
        if (mRadiusInPixel == radiusInPixel
                || 1 > radiusInPixel) {
            return;
        }
        mRadiusInPixel = radiusInPixel;

        runOnDraw(new Runnable() {
            @Override
            public void run() {
//                destroy();
                for (GPUImageFilter filter : mFilters) {
                    filter.destroy();
                }
                mFilters.clear();
                final float minimumWeightToFindEdgeOfSamplingArea = 1.0f/256.0f;
                int sampleRadius = (int) Math.floor(Math.sqrt(
                        -2.0 * Math.pow(mRadiusInPixel, 2.0)
                                * Math.log(minimumWeightToFindEdgeOfSamplingArea * Math.sqrt(2.0 * Math.PI * Math.pow(mRadiusInPixel, 2.0)))
                ));
                String vertexShader = vertexShaderForStandardBlurOfRadius(sampleRadius, radiusInPixel);
                String fragmentShader = fragmentShaderForStandardBlurOfRadius(sampleRadius, radiusInPixel);
                addFilter(new GPUImageFilter(vertexShader, fragmentShader));
                addFilter(new GPUImageFilter(vertexShader, fragmentShader));
                for (GPUImageFilter filter : mFilters) {
                    filter.init();
                }
                updateMergedFilters();
                initTexelOffsets();
                GPUImageFilter filter = mFilters.get(0);
                mWeightsLocation0 = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
                filter = mFilters.get(1);
                mWeightsLocation1 = GLES20.glGetUniformLocation(filter.getProgram(), "blurWeights");
                setWeights(fragmentWeightsForStandardBlurOfRadius(sampleRadius, radiusInPixel));
//                init();
//                GLES20.glUseProgram(getProgram());

            }
        });
    }
}
