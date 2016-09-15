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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.filter.IF1977Filter;
import jp.co.cyberagent.android.gpuimage.filter.IFAmaroFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFBrannanFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFEarlybirdFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFHefeFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFHudsonFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFInkwellFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFLomoFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFLordKelvinFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFNashvilleFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFRiseFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFSierraFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFSutroFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFToasterFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFValenciaFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFWaldenFilter;
import jp.co.cyberagent.android.gpuimage.filter.IFXprollFilter;

public class GPUImageFilterTools {

    public interface FilterAdjuster {
        void adjust(int percentage);
        GPUImageFilter getFilter();
    }

    protected class SimpleAdjuster implements FilterAdjuster {
        private GPUImageFilter filter;

        public SimpleAdjuster(final GPUImageFilter filter) {
            this.filter = filter;
        }

        public GPUImageFilter getFilter() {
            return filter;
        }

        public void adjust(int percentage) {

        }

        protected float range(final int percentage, final float start, final float end) {
            return (end - start) * percentage / 100.0f + start;
        }

        protected int range(final int percentage, final int start, final int end) {
            return (end - start) * percentage / 100 + start;
        }
    }

    private abstract class FilterCreator {
        abstract public GPUImageFilter createFilter(Context context);
        public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
            return null;
        }
    }

    private class FilterWithTextureCreator extends FilterCreator {
        private int resId;
        private Class<? extends GPUImageTwoInputFilter> filterClass;

        FilterWithTextureCreator(
                Class<? extends GPUImageTwoInputFilter> filterClass,
                int resId) {
            super();
            this.filterClass = filterClass;
            this.resId = resId;
        }

        final protected int getResId() {
            return resId;
        }

        public GPUImageFilter createFilter(Context context) {
            try {
                GPUImageTwoInputFilter filter = filterClass.newInstance();
                filter.setBitmap(BitmapFactory.decodeResource(context.getResources(), getResId()));
                return filter;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class FilterItem {
        private String name;
        private FilterCreator creator;

        public FilterItem(String name, FilterCreator creator) {
            this.name = name;
            this.creator = creator;
        }

        public String getName() {
            return name;
        }

        public FilterCreator getCreator() {
            return creator;
        }
    }

    private static GPUImageFilterTools instance;
    private ArrayList<FilterItem> filterItems;

    public static GPUImageFilterTools instance() {
        if (null == instance) {
            instance = new GPUImageFilterTools();
        }
        return instance;
    }

    final public int getCount() {
        return filterItems.size();
    }

    final public String getName(int index) {
        if (0 > index || index >= getCount()) {
            return null;
        }
        return filterItems.get(index).getName();
    }

    final public String[] getNames() {
        String[] names = new String[filterItems.size()];
        int i = 0;
        for (FilterItem item : filterItems) {
            names[i] = item.getName();
            ++i;
        }
        return names;
    }

    final public GPUImageFilter getFilter(int index, Context context) {
        if (0 > index || index >= getCount()) {
            return null;
        }
        return filterItems.get(index).getCreator().createFilter(context);
    }

    final public FilterAdjuster getAdjuster(int index, GPUImageFilter filter) {
        if (0 > index || index >= getCount()) {
            return null;
        }
        return filterItems.get(index).getCreator().createAdjuster(filter);
    }

    public ArrayList<FilterItem> getFilters() {
        return filterItems;
    }

    private GPUImageFilterTools() {

        filterItems = new ArrayList<>(120);

        filterItems.add(new FilterItem("ZoomBlur", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageZoomBlurFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImageZoomBlurFilter) getFilter()).setSize(range(percentage, 1.0f, 20.0f));
                    }
                };
            }
        }));

        filterItems.add(new FilterItem("Halftone", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageHalftoneFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImageHalftoneFilter) getFilter()).setFractionalWidthOfAPixel(range(percentage, 0.0f, 1.0f));
                    }
                };
            }
        }));

        filterItems.add(new FilterItem("Rise", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFRiseFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Hudson", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFHudsonFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Xproll", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFXprollFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Sierra", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFSierraFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Lomo", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFLomoFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Earlybird", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFEarlybirdFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Sutro", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFSutroFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Toaster", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFToasterFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Brannan", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFBrannanFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Inkwell", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFInkwellFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Walden", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFWaldenFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Hefe", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFHefeFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Valencia", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFValenciaFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Nashville", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFNashvilleFilter(context);
            }
        }));

        filterItems.add(new FilterItem("1977", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IF1977Filter(context);
            }
        }));

        filterItems.add(new FilterItem("Kelvin", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFLordKelvinFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Nashville", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new IFAmaroFilter(context);
            }
        }));

        filterItems.add(new FilterItem("Negative", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.color_negative));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("CrossRGB", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.cross_process));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("Darker", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.darker));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("AddContrast", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.increase_contrast));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("Lighter", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.lighter));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("LinearContrast", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.linear_contrast));
                return filter;
            }
        }));

        // Medium Contrast
        filterItems.add(new FilterItem("MedContrast", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.medium_contrast));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("XRay", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.negative));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("StrongContrast", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.strong_contrast));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("CrossProcess", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.crossprocess));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("PurpleGreen", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.purple_green));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("Aqua", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.aqua));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("YellowRed", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.yellow_red));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("PureMemory", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.pure_memory));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("YellowBlue", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.yellow_blue));
                return filter;
            }
        }));

        filterItems.add(new FilterItem("DarkBlue", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                filter.setFromCurveFileInputStream(
                        context.getResources().openRawResource(R.raw.dark_blue));
                return filter;
            }
        }));

        // TODO: Implement SoftElegance filter
//        filterItems.add(new FilterItem("SoftElegance", new FilterCreator() {
//            @Override
//            public GPUImageFilter createFilter(Context context) {
//
//            }
//        }));

        // TODO: Implement MissEtikate filter

        // TODO: Implement Amatorka filter


        filterItems.add(new FilterItem("ToneCurve", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                GPUImageToneCurveFilter filter = new GPUImageToneCurveFilter();
                PointF blueCtrl[] = new PointF[3];
                blueCtrl[0] = new PointF(0.0f, 0.0f);
                blueCtrl[1] = new PointF(0.5f, 0.5f);
                blueCtrl[2] = new PointF(1.0f, 0.75f);
                filter.setBlueControlPoints(blueCtrl);
                return filter;
            }
        }));

        filterItems.add(new FilterItem("Sepia", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageSepiaFilter();
            }
        }));

        filterItems.add(new FilterItem("Sketch", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageSketchFilter();
            }
        }));

        filterItems.add(new FilterItem("Toon", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageToonFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImageToonFilter) getFilter()).setThreshold(range(percentage, 0.0f, 1.0f));
                    }
                };
            }
        }));

        filterItems.add(new FilterItem("Pixelation", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImagePixelationFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImagePixelationFilter) getFilter()).setPixel(range(percentage, 0.1f, 20.0f));
                    }
                };
            }
        }));

        // TODO: Implement PolkaDot filter

        filterItems.add(new FilterItem("CGA", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageCGAColorspaceFilter();
            }
        }));

        filterItems.add(new FilterItem("Emboss", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageEmbossFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImageEmbossFilter) getFilter()).setIntensity(range(percentage, 0.0f, 4.0f));
                    }
                };
            }
        }));

        filterItems.add(new FilterItem("Posterize", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImagePosterizeFilter();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImagePosterizeFilter) getFilter()).setColorLevels((int) range(percentage, 0.0f, 255.0f));
                    }
                };
            }
        }));

        filterItems.add(new FilterItem("Dilation", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageDilationFilter(3);
            }
        }));

        // TODO: Implement Erosion filter

        // TODO: Implement Openning filter

        // TODO: Implement Closing filter

        // TODO: Implement Median filter


        filterItems.add(new FilterItem("ColorInvert", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageColorInvertFilter();
            }
        }));

        filterItems.add(new FilterItem("Grayscale", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageGrayscaleFilter();
            }
        }));

        filterItems.add(new FilterItem("FalseColor", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageFalseColorFilter();
            }
        }));

        // TODO: Implement Luminance Threshold filter

        filterItems.add(new FilterItem("SobelEdge", new FilterCreator() {
            @Override
            public GPUImageFilter createFilter(Context context) {
                return new GPUImageSobelEdgeDetection();
            }
            @Override
            public FilterAdjuster createAdjuster(final GPUImageFilter filter) {
                return new SimpleAdjuster(filter) {
                    @Override
                    public void adjust(int percentage) {
                        ((GPUImageSobelEdgeDetection) getFilter()).setLineSize(range(percentage, 1.0f, 10.0f));
                    }
                };
            }
        }));

        // TODO: Implement PrewittEdgeDetection filter

        // TODO: Implement Mosaic filter

        for (int i = 1; i <= 27; ++i) {
            final String name = String.format("Bokeh_%02d", i);
            filterItems.add(new FilterItem(name,
                    new FilterWithTextureCreator(
                            GPUImageScreenBlendFilter.class,
                            R.drawable.ic_bokeh_01 + i - 1)));
        }

        final byte HardLightBlend = 0;
        final byte MultiplyBlend = 1;
        final byte OverlayBlend = 2;
        final byte ColorBurnBlend = 3;
        final byte DissolveBlend = 4;

        final byte FILTER_TYPE[] = {
                HardLightBlend,   // 01
                DissolveBlend,    // 02
                MultiplyBlend,    // 03
                MultiplyBlend,    // 04
                MultiplyBlend,    // 05
                MultiplyBlend,    // 06
                MultiplyBlend,    // 07
                MultiplyBlend,    // 08
                MultiplyBlend,    // 09
                HardLightBlend,   // 10
                MultiplyBlend,    // 11
                MultiplyBlend,    // 12
                MultiplyBlend,    // 13
                MultiplyBlend,    // 14
                MultiplyBlend,    // 15
                MultiplyBlend,    // 16
                HardLightBlend,   // 17
                MultiplyBlend,    // 18
                HardLightBlend,   // 19
                OverlayBlend,     // 20
                MultiplyBlend,    // 21
                OverlayBlend,     // 22
                HardLightBlend,   // 23
                MultiplyBlend,    // 24
                OverlayBlend,     // 25
                OverlayBlend,     // 26
                OverlayBlend,     // 27
                DissolveBlend,    // 28
                HardLightBlend,   // 29
                MultiplyBlend,    // 30
                MultiplyBlend,    // 31
                MultiplyBlend,    // 32
                HardLightBlend,   // 33
                ColorBurnBlend,   // 34
                MultiplyBlend,    // 35
                MultiplyBlend,    // 36
                MultiplyBlend,    // 37
                MultiplyBlend,    // 38
                DissolveBlend,    // 39
                MultiplyBlend,    // 40
                ColorBurnBlend,   // 41
        };
        for (int i = 0; i < FILTER_TYPE.length; ++i) {
            final String name = String.format("Texture_%02d", i + 1);
            Class<? extends GPUImageTwoInputFilter> filterClass;
            switch (FILTER_TYPE[i]) {
                case HardLightBlend:
                    filterClass = GPUImageHardLightBlendFilter.class;
                    break;
                case MultiplyBlend:
                    filterClass = GPUImageMultiplyBlendFilter.class;
                    break;
                case OverlayBlend:
                    filterClass = GPUImageOverlayBlendFilter.class;
                    break;
                case ColorBurnBlend:
                    filterClass = GPUImageColorBurnBlendFilter.class;
                    break;
                case DissolveBlend:
                    filterClass = GPUImageDissolveBlendFilter.class;
                    break;
                default:
                    continue;
            }
            filterItems.add(new FilterItem(name,
                    new FilterWithTextureCreator(
                            filterClass, R.drawable.ic_texture_01 + i)));
        }
    }

}
