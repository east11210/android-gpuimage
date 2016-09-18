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

public class GPUImageColorBurnBlendFilter extends GPUImageTwoInputFilter {
    public static final String COLOR_BURN_BLEND_FRAGMENT_SHADER =
            "varying highp vec2 textureCoordinate;\n" +
            "varying highp vec2 textureCoordinate2;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "uniform sampler2D inputImageTexture2;\n" +
            "uniform int swapTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "   mediump vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "   mediump vec4 textureColor2 = texture2D(inputImageTexture2, textureCoordinate2);\n" +
            "   mediump vec4 whiteColor = vec4(1.0);\n" +
            "   if (0 == swapTexture) {\n" +
            "      gl_FragColor = whiteColor - (whiteColor - textureColor) / textureColor2;\n" +
            "   } else {\n" +
            "      gl_FragColor = whiteColor - (whiteColor - textureColor2) / textureColor;\n" +
            "   }\n" +
            "}";

    public GPUImageColorBurnBlendFilter() {
        this(1);
    }

    public GPUImageColorBurnBlendFilter(final int swapTexture) {
        super(COLOR_BURN_BLEND_FRAGMENT_SHADER);
        mSwapTexture = swapTexture;
    }

    private int mSwapTexture;
    private int mSwapTextureLocation;

    @Override
    public void onInit() {
        super.onInit();
        mSwapTextureLocation = GLES20.glGetUniformLocation(getProgram(), "swapTexture");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setSwapTexture(mSwapTexture);
    }

    public void setSwapTexture(final int swapTexture) {
        if (0 != swapTexture && 1 != swapTexture) {
            return;
        }
        mSwapTexture = swapTexture;
        setInteger(mSwapTextureLocation, mSwapTexture);
    }
}

// TODO: Add parameter to swap texture1 and texture2