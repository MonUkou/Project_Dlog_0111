package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Post;
import com.example.myapplication.view.ZoomDismissImageView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class PhotoViewerActivity extends AppCompatActivity {
    private static final String EXTRA_URLS = "photo_urls";
    private static final String EXTRA_INDEX = "photo_index";
    private static final String EXTRA_METADATA = "photo_metadata";

    private final ArrayList<String> urls = new ArrayList<>();
    private final ArrayList<String> metadata = new ArrayList<>();
    private int index;
    private ZoomDismissImageView photo;
    private TextView dots;
    private ImageButton previous;
    private ImageButton next;

    public static void open(Context context, Post post, int startIndex) {
        if (context == null || post == null) return;
        ArrayList<String> images = new ArrayList<>();
        if (post.getImageUrls() != null) images.addAll(post.getImageUrls());
        if (images.isEmpty() && post.getImageUrl() != null) images.add(post.getImageUrl());
        if (images.isEmpty()) return;
        ArrayList<String> specs = new ArrayList<>();
        for (Map<String, String> item : post.getPhotoMetadata()) specs.add(new JSONObject(item).toString());
        Intent intent = new Intent(context, PhotoViewerActivity.class);
        intent.putStringArrayListExtra(EXTRA_URLS, images);
        intent.putStringArrayListExtra(EXTRA_METADATA, specs);
        intent.putExtra(EXTRA_INDEX, Math.max(0, Math.min(startIndex, images.size() - 1)));
        context.startActivity(intent);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        ArrayList<String> received = getIntent().getStringArrayListExtra(EXTRA_URLS);
        ArrayList<String> receivedMetadata = getIntent().getStringArrayListExtra(EXTRA_METADATA);
        if (received != null) urls.addAll(received);
        if (receivedMetadata != null) metadata.addAll(receivedMetadata);
        if (urls.isEmpty()) { finish(); return; }
        index = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_INDEX, 0), urls.size() - 1));

        photo = findViewById(R.id.zoomPhoto);
        dots = findViewById(R.id.tvViewerDots);
        previous = findViewById(R.id.btnViewerPrevious);
        next = findViewById(R.id.btnViewerNext);
        photo.setGestureCallback(new ZoomDismissImageView.GestureCallback() {
            @Override public void onDismiss() { finish(); }
            @Override public void onPrevious() { move(-1); }
            @Override public void onNext() { move(1); }
        });
        findViewById(R.id.btnCloseViewer).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        findViewById(R.id.btnPhotoInfo).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showMetadata(); }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { move(-1); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { move(1); }
        });
        showPhoto();
    }

    private void move(int direction) {
        int target = index + direction;
        if (target < 0 || target >= urls.size()) return;
        index = target;
        showPhoto();
    }

    private void showPhoto() {
        photo.resetZoom();
        Glide.with(this).load(urls.get(index)).fitCenter().into(photo);
        previous.setVisibility(index > 0 ? View.VISIBLE : View.GONE);
        next.setVisibility(index + 1 < urls.size() ? View.VISIBLE : View.GONE);
        StringBuilder indicator = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) indicator.append(' ');
            indicator.append(i == index ? '●' : '○');
        }
        dots.setText(indicator.toString());
        dots.setVisibility(urls.size() > 1 ? View.VISIBLE : View.GONE);
        photo.setContentDescription("Ảnh " + (index + 1) + " trên " + urls.size()
                + ". Có thể chụm để phóng to hoặc vuốt xuống để đóng.");
    }

    private void showMetadata() {
        StringBuilder text = new StringBuilder();
        if (index < metadata.size()) {
            try {
                JSONObject object = new JSONObject(metadata.get(index));
                Iterator<String> keys = object.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    text.append(key).append(": ").append(object.optString(key)).append('\n');
                }
            } catch (Exception ignored) { }
        }
        if (text.length() == 0) text.append("Ảnh này không có thông số EXIF khả dụng.");
        new AlertDialog.Builder(this).setTitle("Thông số ảnh")
                .setMessage(text.toString().trim()).setPositiveButton("Đóng", null).show();
    }
}
