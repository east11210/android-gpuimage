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
 * Changes the contrast of the image.<br>
 * <br>
 * blurSize A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
 * blurCenter The normalized center of the blur. (0.5, 0.5) by default
 */
public class GPUImageZoomBlurFilter extends GPUImageFilter {
    public static final String CONTRAST_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform highp vec2 blurCenter;\n" +
            " uniform highp float blurSize;\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "     highp vec2 samplingOffset = 1.0/100.0 * (blurCenter - textureCoordinate) * blurSize;\n" +
            "     lowp vec4 fragmentColor = texture2D(inputImageTexture, textureCoordinate) * 0.18;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate + samplingOffset) * 0.15;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate + (2.0 * samplingOffset)) *  0.12;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate + (3.0 * samplingOffset)) * 0.09;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate + (4.0 * samplingOffset)) * 0.05;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate - samplingOffset) * 0.15;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate - (2.0 * samplingOffset)) *  0.12;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate - (3.0 * samplingOffset)) * 0.09;\n" +
            "     fragmentColor += texture2D(inputImageTexture, textureCoordinate - (4.0 * samplingOffset)) * 0.05;\n" +
            "\n" +
            "     gl_FragColor = fragmentColor;\n" +
            " }";

    private PointF mCenter;
    private int mCenterLocation;
    private float mSize;
    private int mSizeLocation;

    public GPUImageZoomBlurFilter() {
        this(new PointF(0.5f, 0.5f), 1.0f);
    }

    public GPUImageZoomBlurFilter(PointF center, float size) {
        super(NO_FILTER_VERTEX_SHADER, CONTRAST_FRAGMENT_SHADER);
        mCenter = center;
        mSize = size;
    }

    @Override
    public void onInit() {
        super.onInit();
        mCenterLocation = GLES20.glGetUniformLocation(getProgram(), "blurCenter");
        mSizeLocation = GLES20.glGetUniformLocation(getProgram(), "blurSize");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setCenter(mCenter);
        setSize(mSize);
    }

    public void setCenter(final PointF center) {
        mCenter = center;
        setPoint(mCenterLocation, mCenter);
    }

    public void setSize(final float size) {
        mSize = size;
        setFloat(mSizeLocation, mSize);
    }
}
