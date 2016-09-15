package jp.co.cyberagent.android.gpuimage.sample.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterTools;

/**
 * Created by Administrator on 2016/9/15.
 */
public class GPUImageFilterChooser {

    public interface OnGpuImageFilterChosenListener {
        void onGpuImageFilterChosenListener(final GPUImageFilter filter,
                                            final GPUImageFilterTools.FilterAdjuster adjuster);
    }

    public static void showDialog(final Context context,
                                  final OnGpuImageFilterChosenListener listener) {

        final GPUImageFilterTools filterTools = GPUImageFilterTools.instance();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose a filter");
        builder.setItems(filterTools.getNames(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int item) {
                        GPUImageFilter filter = filterTools.getFilter(item, context);
                        listener.onGpuImageFilterChosenListener(
                                filter,
                                filterTools.getAdjuster(item, filter));
                    }
                });
        builder.create().show();
    }
}
