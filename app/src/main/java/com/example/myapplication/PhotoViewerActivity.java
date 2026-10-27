package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Post;
import com.example.myapplication.view.ZoomDismissImageView;
import com.example.myapplication.service.impl.PostService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class PhotoViewerActivity extends AppCompatActivity {
    private static final String EXTRA_URLS = "photo_urls";
    private static final String EXTRA_INDEX = "photo_index";
    private static final String EXTRA_METADATA = "photo_metadata";
    private static final String EXTRA_POST_ID = "photo_post_id";
    private static final String EXTRA_AUTHOR_ID = "photo_author_id";

    private final ArrayList<String> urls = new ArrayList<>();
    private final ArrayList<String> metadata = new ArrayList<>();
    private int index;
    private ZoomDismissImageView photo;
    private TextView dots;
    private ImageButton previous;
    private ImageButton next;
    private String postId;
    private String authorId;

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
        intent.putExtra(EXTRA_POST_ID, post.getId());
        intent.putExtra(EXTRA_AUTHOR_ID, post.getAuthorId());
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
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        authorId = getIntent().getStringExtra(EXTRA_AUTHOR_ID);

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
        findViewById(R.id.btnViewerDownload).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { downloadCurrentPhoto(); }
        });
        findViewById(R.id.btnViewerReport).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { chooseReportReason(); }
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

    private void downloadCurrentPhoto() {
        final String url = urls.get(index);
        PostService.getInstance().saveImageToDevice(this, url, new PostService.Callback<String>() {
            @Override public void onSuccess(String message) {
                Toast.makeText(PhotoViewerActivity.this, message, Toast.LENGTH_LONG).show();
            }
            @Override public void onError(Exception error) {
                Toast.makeText(PhotoViewerActivity.this,
                        "Không thể tải ảnh: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void chooseReportReason() {
        if (postId == null || postId.isEmpty() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Cần đăng nhập và mở ảnh từ bài đăng để báo cáo.", Toast.LENGTH_LONG).show();
            return;
        }
        final String[] reasons = {"Nội dung không phù hợp", "Vi phạm quyền ảnh", "Spam hoặc lừa đảo", "Lý do khác"};
        new AlertDialog.Builder(this).setTitle("Báo cáo ảnh " + (index + 1))
                .setItems(reasons, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        submitReport(reasons[which]);
                    }
                }).setNegativeButton("Hủy", null).show();
    }

    private void submitReport(String reason) {
        Map<String, Object> report = new HashMap<>();
        report.put("type", "image");
        report.put("postId", postId);
        report.put("authorId", authorId == null ? "" : authorId);
        report.put("reporterId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        report.put("imageIndex", index);
        report.put("imageUrl", urls.get(index));
        report.put("reason", reason);
        report.put("status", "open");
        report.put("createdAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("reports").add(report)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override public void onSuccess(com.google.firebase.firestore.DocumentReference reference) {
                        Toast.makeText(PhotoViewerActivity.this, "Đã gửi báo cáo ảnh cho quản trị viên.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@androidx.annotation.NonNull Exception error) {
                        Toast.makeText(PhotoViewerActivity.this, "Không gửi được báo cáo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
