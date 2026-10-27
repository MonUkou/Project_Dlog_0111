package com.example.myapplication.util;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.widget.AdapterView;
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
            .setItems(new String[]{"Ảnh trên thiết bị / Google Drive", "Thư viện ảnh DLog"},
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) devicePicker.run(); else showLibrary(context, selected);
                    }
                }).setNegativeButton("Hủy", null).show();
    }
    private static void showLibrary(Context context, Consumer<Uri> selected) {
        GridView grid = new GridView(context);
        grid.setNumColumns(3);
        int gap = (int)(8 * context.getResources().getDisplayMetrics().density);
        grid.setPadding(gap, gap, gap, gap);
        grid.setHorizontalSpacing(gap); grid.setVerticalSpacing(gap);
        grid.setAdapter(new BaseAdapter() {
            public int getCount() { return PhotoLibrary.size(); }
            public Object getItem(int position) { return PhotoLibrary.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View recycled, ViewGroup parent) {
                ImageView image = recycled instanceof ImageView ? (ImageView)recycled : new ImageView(context);
                image.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, gap * 13));
                PhotoLibrary.Photo photo = PhotoLibrary.get(position);
                image.setContentDescription("Chọn ảnh " + photo.title + ", " + photo.category);
                Glide.with(context).load(photo.drawableId).centerCrop().into(image);
                return image;
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("Thư viện ảnh DLog · " + PhotoLibrary.size() + " ảnh")
            .setView(grid).setNegativeButton("Hủy", null).create();
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selected.accept(PhotoLibrary.uri(context, position));
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
