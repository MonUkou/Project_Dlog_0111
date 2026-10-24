package com.example.myapplication.util;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.BaseAdapter;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import java.util.function.Consumer;

/** Bundled library is available offline; personal photos use Android's document picker. */
public final class ImagePickerDialog {
    public static void show(Context context, Runnable devicePicker, Consumer<Uri> selected) {
        new AlertDialog.Builder(context).setTitle("Chọn hình ảnh")
            .setItems(new String[]{"Ảnh trên thiết bị / Google Drive", "Thư viện ảnh DLog"}, (dialog, which) -> {
                if (which == 0) devicePicker.run(); else showLibrary(context, selected);
            }).setNegativeButton("Hủy", null).show();
    }
    private static void showLibrary(Context context, Consumer<Uri> selected) {
        GridView grid = new GridView(context);
        grid.setNumColumns(3);
        int gap = (int)(8 * context.getResources().getDisplayMetrics().density);
        grid.setPadding(gap, gap, gap, gap);
        grid.setHorizontalSpacing(gap); grid.setVerticalSpacing(gap);
        grid.setAdapter(new BaseAdapter() {
            public int getCount() { return 25; }
            public Object getItem(int position) { return uri(context, position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View recycled, ViewGroup parent) {
                ImageView image = recycled instanceof ImageView ? (ImageView)recycled : new ImageView(context);
                image.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, gap * 13));
                image.setContentDescription("Chọn ảnh thư viện " + (position + 1));
                Glide.with(context).load(uri(context, position)).centerCrop().into(image);
                return image;
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("Thư viện ảnh DLog")
            .setView(grid).setNegativeButton("Hủy", null).create();
        grid.setOnItemClickListener((parent, view, position, id) -> { selected.accept(uri(context, position)); dialog.dismiss(); });
        dialog.show();
    }
    private static Uri uri(Context context, int position) {
        String name = String.format(java.util.Locale.ROOT, "library_photo_%02d", position + 1);
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
    }
}
