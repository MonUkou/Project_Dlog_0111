package com.example.myapplication.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/** Compact album preview. The full album opens when any tile is selected. */
public class PhotoCollageView extends LinearLayout {
    public interface OnPhotoClickListener {
        void onPhotoClick(int index);
    }

    public PhotoCollageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
    }

    public void show(List<String> urls, OnPhotoClickListener listener) {
        removeAllViews();
        int count = urls == null ? 0 : urls.size();
        setOrientation(count >= 4 ? VERTICAL : HORIZONTAL);
        if (count == 0) {
            TextView empty = new TextView(getContext());
            empty.setText("Chưa có ảnh");
            empty.setTextColor(Color.WHITE);
            empty.setGravity(Gravity.CENTER);
            addView(empty, new LayoutParams(-1, -1));
        } else if (count == 1) {
            addTile(this, urls, 0, false, listener);
        } else if (count == 2) {
            addTile(this, urls, 0, false, listener);
            addTile(this, urls, 1, false, listener);
        } else if (count == 3) {
            addTile(this, urls, 0, false, listener);
            LinearLayout right = group(VERTICAL);
            addTile(right, urls, 1, false, listener);
            addTile(right, urls, 2, false, listener);
            addView(right, cellParams());
        } else {
            LinearLayout top = group(HORIZONTAL);
            LinearLayout bottom = group(HORIZONTAL);
            addTile(top, urls, 0, false, listener);
            addTile(top, urls, 1, false, listener);
            addTile(bottom, urls, 2, false, listener);
            addTile(bottom, urls, 3, true, listener);
            addView(top, cellParams());
            addView(bottom, cellParams());
        }
    }

    private LinearLayout group(int orientation) {
        LinearLayout group = new LinearLayout(getContext());
        group.setOrientation(orientation);
        return group;
    }

    private LayoutParams cellParams() {
        LayoutParams params = getOrientation() == HORIZONTAL
                ? new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                : new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        int gap = Math.round(getResources().getDisplayMetrics().density);
        params.setMargins(gap, gap, gap, gap);
        return params;
    }

    private void addTile(LinearLayout parent, List<String> urls, final int index,
                         boolean overflow, final OnPhotoClickListener listener) {
        FrameLayout tile = new FrameLayout(getContext());
        ImageView image = new ImageView(getContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (urls.get(index) != null && !urls.get(index).isEmpty()) {
            Glide.with(getContext()).load(urls.get(index)).centerCrop().into(image);
        }
        if (overflow) {
            View shade = new View(getContext());
            shade.setBackgroundColor(0x990B1D32);
            tile.addView(shade, new FrameLayout.LayoutParams(-1, -1));
            TextView extra = new TextView(getContext());
            extra.setGravity(Gravity.CENTER);
            extra.setText("+" + (urls.size() - 3));
            extra.setTextColor(Color.WHITE);
            extra.setTextSize(27);
            extra.setTypeface(null, Typeface.BOLD);
            tile.addView(extra, new FrameLayout.LayoutParams(-1, -1));
        }
        tile.setContentDescription("Xem ảnh " + (index + 1) + " trong " + urls.size() + " ảnh");
        tile.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (listener != null) listener.onPhotoClick(index);
            }
        });
        LayoutParams params = parent.getOrientation() == HORIZONTAL
                ? new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                : new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        int gap = Math.round(getResources().getDisplayMetrics().density);
        params.setMargins(gap, gap, gap, gap);
        parent.addView(tile, params);
    }
}
