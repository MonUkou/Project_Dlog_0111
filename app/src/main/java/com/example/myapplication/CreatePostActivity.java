package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView btnBackCreate;
    private Button btnSubmitPost;
    private FrameLayout btnSelectImage;
    private ImageView ivSelectedImage;
    private LinearLayout llImagePlaceholder;
    private TextView btnUseSampleImage;
    private EditText etCaption;
    private LinearLayout llLoadingCreate;

    private Uri selectedImageUri;
    private PostService postService;
    private AuthService authService;

    private static final String KEY_IMAGE_URI = "saved_image_uri";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    showSelectedImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        postService = PostService.getInstance();
        authService = AuthService.getInstance();

        initViews();

        if (savedInstanceState != null) {
            String uriStr = savedInstanceState.getString(KEY_IMAGE_URI);
            if (uriStr != null) {
                selectedImageUri = Uri.parse(uriStr);
                showSelectedImage();
            }
        }
    }

    private void initViews() {
        btnBackCreate = findViewById(R.id.btnBackCreate);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        llImagePlaceholder = findViewById(R.id.llImagePlaceholder);
        btnUseSampleImage = findViewById(R.id.btnUseSampleImage);
        etCaption = findViewById(R.id.etCaption);
        llLoadingCreate = findViewById(R.id.llLoadingCreate);

        btnBackCreate.setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> openGallery());

        btnUseSampleImage.setOnClickListener(v -> {
            selectedImageUri = Uri.parse("https://picsum.photos/800/800");
            showSelectedImage();
            Toast.makeText(this, "Đã chọn ảnh mẫu thử nghiệm!", Toast.LENGTH_SHORT).show();
        });

        btnSubmitPost.setOnClickListener(v -> handleCreatePost());
    }

    private void openGallery() {
        try {
            imagePickerLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSelectedImage() {
        if (selectedImageUri != null) {
            llImagePlaceholder.setVisibility(View.GONE);
            ivSelectedImage.setVisibility(View.VISIBLE);
            try {
                ivSelectedImage.setImageURI(selectedImageUri);
            } catch (Exception ignored) {}
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .placeholder(R.drawable.bg_rounded_edittext)
                    .into(ivSelectedImage);
        }
    }

    private void handleCreatePost() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn hình ảnh trước khi đăng bài!", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = etCaption.getText().toString().trim();
        String currentUserId = authService.getCurrentUserId();

        // Show loading & disable submit button
        llLoadingCreate.setVisibility(View.VISIBLE);
        btnSubmitPost.setEnabled(false);

        postService.createPost(currentUserId, caption, selectedImageUri, this, new PostService.Callback<Post>() {
            @Override
            public void onSuccess(Post post) {
                llLoadingCreate.setVisibility(View.GONE);
                btnSubmitPost.setEnabled(true);

                Toast.makeText(CreatePostActivity.this, "Đăng bài viết thành công!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CreatePostActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                llLoadingCreate.setVisibility(View.GONE);
                btnSubmitPost.setEnabled(true);

                Toast.makeText(CreatePostActivity.this, "Lỗi đăng bài: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedImageUri != null) {
            outState.putString(KEY_IMAGE_URI, selectedImageUri.toString());
        }
    }
}
