
package com.example.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.model.User;
import com.example.myapplication.util.HashtagParser;
import com.google.firebase.firestore.ListenerRegistration;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView btnBackCreate;
    private Button btnSubmitPost;
    private FrameLayout btnSelectImage;
    private ImageView ivSelectedImage;
    private LinearLayout llImagePlaceholder;
    private EditText etCaption;
    private EditText etHashtags;
    private TextView tvSelectedCount;
    private Button btnEditSelectedImage;
    private LinearLayout llLoadingCreate;

    private Uri selectedImageUri;
    private final ArrayList<Uri> selectedImages = new ArrayList<>();

    private PostService postService;
    private AuthService authService;
    private boolean posting;
    private int orientationBeforePost;
    private ListenerRegistration currentUserRegistration;

    private static final String KEY_IMAGE_URI = "saved_image_uri";
    private static final String KEY_IMAGE_URIS = "saved_image_uris";

    private final ActivityResultLauncher<String[]> multiPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            new androidx.activity.result.ActivityResultCallback<List<Uri>>() {
                @Override public void onActivityResult(List<Uri> uris) {
                    if (uris == null || uris.isEmpty()) return;
                    selectedImages.clear();
                    for (Uri uri : uris) {
                        if (selectedImages.size() == 10) break;
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) { }
                        selectedImages.add(uri);
                    }
                    selectedImageUri = selectedImages.get(0);
                    showSelectedImage();
                }
            });

    private final ActivityResultLauncher<Intent> editorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new androidx.activity.result.ActivityResultCallback<androidx.activity.result.ActivityResult>() {
                @Override public void onActivityResult(androidx.activity.result.ActivityResult result) {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    String value = result.getData().getStringExtra(PhotoEditorActivity.EXTRA_IMAGE_URI);
                    if (value == null || value.isEmpty()) return;
                    Uri edited = Uri.parse(value);
                    if (selectedImages.isEmpty()) selectedImages.add(edited); else selectedImages.set(0, edited);
                    selectedImageUri = edited;
                    showSelectedImage();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        postService = PostService.getInstance();
        authService = AuthService.getInstance();
        authService.init(this);

        initViews();
        loadCurrentAvatar();

        if (savedInstanceState != null) {
            ArrayList<String> savedUris = savedInstanceState.getStringArrayList(KEY_IMAGE_URIS);
            if (savedUris != null) {
                for (String savedUri : savedUris) selectedImages.add(Uri.parse(savedUri));
            }
            String uriString = savedInstanceState.getString(KEY_IMAGE_URI);

            if (uriString != null && !uriString.isEmpty()) {
                selectedImageUri = Uri.parse(uriString);
                showSelectedImage();
            }
        }
    }

    private void loadCurrentAvatar() {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;
        if (currentUserRegistration != null) return;
        currentUserRegistration = UserService.getInstance().observeUserProfile(userId, new UserService.Callback<User>() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed() || user == null) return;
                ImageView avatar = findViewById(R.id.ivCreateAvatar);
                Glide.with(CreatePostActivity.this).load(user.getAvatarUrl())
                        .placeholder(R.drawable.bg_circle_avatar)
                        .error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
            }

            @Override public void onError(Exception error) {
                // The default avatar remains visible while offline.
            }
        });
    }

    private void initViews() {
        btnBackCreate = findViewById(R.id.btnBackCreate);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        llImagePlaceholder = findViewById(R.id.llImagePlaceholder);
        etCaption = findViewById(R.id.etCaption);
        etHashtags = findViewById(R.id.etHashtags);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnEditSelectedImage = findViewById(R.id.btnEditSelectedImage);
        llLoadingCreate = findViewById(R.id.llLoadingCreate);

        btnBackCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (posting) {
                    Toast.makeText(CreatePostActivity.this, "Ảnh đang được tải lên, vui lòng đợi.", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                multiPickerLauncher.launch(new String[]{"image/*"});
            }
        });

        btnSubmitPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCreatePost();
            }
        });

        btnEditSelectedImage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (selectedImages.isEmpty()) return;
                Intent intent = new Intent(CreatePostActivity.this, PhotoEditorActivity.class);
                intent.putExtra(PhotoEditorActivity.EXTRA_IMAGE_URI, selectedImages.get(0).toString());
                editorLauncher.launch(intent);
            }
        });

        // Không còn các nút chọn ảnh mẫu có sẵn.
    }

    private void showSelectedImage() {
        if (selectedImageUri == null) {
            return;
        }

        llImagePlaceholder.setVisibility(View.GONE);
        ivSelectedImage.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(selectedImageUri)
                .centerCrop()
                .into(ivSelectedImage);
        if (selectedImages.isEmpty()) selectedImages.add(selectedImageUri);
        tvSelectedCount.setText(selectedImages.size() + " ảnh đã chọn · Nhấn ảnh để chọn lại");
        btnEditSelectedImage.setVisibility(View.VISIBLE);
    }

    private void handleCreatePost() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(
                    this,
                    "Vui lòng chọn hình ảnh trước khi đăng bài!",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String caption = etCaption.getText().toString().trim();
        String currentUserId = authService.getCurrentUserId();

        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (!btnSubmitPost.isEnabled()) return;
        posting = true;
        orientationBeforePost = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        btnSelectImage.setEnabled(false);
        btnBackCreate.setEnabled(false);
        llLoadingCreate.setVisibility(View.VISIBLE);
        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText("Đang đăng");

        List<String> hashtags = HashtagParser.extract(
                caption, etHashtags.getText().toString());
        postService.createPost(
                currentUserId,
                caption,
                new ArrayList<Uri>(selectedImages),
                hashtags,
                this,
                new PostService.Callback<Post>() {
                    @Override
                    public void onSuccess(Post post) {
                        posting = false;
                        llLoadingCreate.setVisibility(View.GONE);
                        btnSubmitPost.setEnabled(true);
                        btnSelectImage.setEnabled(true);

                        Toast.makeText(
                                CreatePostActivity.this,
                                "Đăng bài viết thành công!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                CreatePostActivity.this,
                                HomeActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );
                        intent.putExtra("refresh_after_post", true);

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        posting = false;
                        setRequestedOrientation(orientationBeforePost);
                        llLoadingCreate.setVisibility(View.GONE);
                        btnSubmitPost.setEnabled(true);
                        btnSubmitPost.setText("Đăng");
                        btnSelectImage.setEnabled(true);
                        btnBackCreate.setEnabled(true);

                        String message;

                        if (e == null || e.getMessage() == null) {
                            message = "Không thể đăng bài. Vui lòng thử lại.";
                        } else {
                            message = e.getMessage();
                        }

                        Toast.makeText(
                                CreatePostActivity.this,
                                "Lỗi đăng bài: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        if (posting) {
            Toast.makeText(this, "Ảnh đang được tải lên, vui lòng đợi.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (selectedImageUri != null) {
            outState.putString(
                    KEY_IMAGE_URI,
                    selectedImageUri.toString()
            );
        }
        ArrayList<String> savedUris = new ArrayList<>();
        for (Uri uri : selectedImages) savedUris.add(uri.toString());
        outState.putStringArrayList(KEY_IMAGE_URIS, savedUris);
    }

    @Override
    protected void onDestroy() {
        if (currentUserRegistration != null) {
            currentUserRegistration.remove();
            currentUserRegistration = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentAvatar();
    }
}
