
package com.example.myapplication;

import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.model.User;
import com.example.myapplication.util.HashtagParser;
import com.example.myapplication.util.ImagePickerDialog;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.function.Consumer;

public class CreatePostActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_POST_ID = "edit_post_id";

    private ImageView btnBackCreate;
    private Button btnSubmitPost;
    private FrameLayout btnSelectImage;
    private LinearLayout photoCollage;
    private LinearLayout llImagePlaceholder;
    private EditText etCaption;
    private EditText etHashtags;
    private TextView tvSelectedCount;
    private TextView btnEditSelectedImage;
    private TextView btnAddMoreImages;
    private LinearLayout llLoadingCreate;

    private Uri selectedImageUri;
    private final ArrayList<Uri> selectedImages = new ArrayList<>();
    private int selectedEditIndex = -1;

    private PostService postService;
    private AuthService authService;
    private boolean posting;
    private String editingPostId;
    private boolean editLoaded;
    private int orientationBeforePost;
    private ListenerRegistration currentUserRegistration;

    private static final String KEY_IMAGE_URI = "saved_image_uri";
    private static final String KEY_IMAGE_URIS = "saved_image_uris";
    private static final String KEY_EDIT_LOADED = "edit_loaded";
    private static final String KEY_EDIT_IMAGE_INDEX = "edit_image_index";

    private final ActivityResultLauncher<String[]> multiPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            new androidx.activity.result.ActivityResultCallback<List<Uri>>() {
                @Override public void onActivityResult(List<Uri> uris) {
                    if (uris == null || uris.isEmpty()) return;
                    int lastAddedIndex = -1;
                    for (Uri uri : uris) {
                        if (selectedImages.size() == 10) break;
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) { }
                        if (!selectedImages.contains(uri)) {
                            selectedImages.add(uri);
                            lastAddedIndex = selectedImages.size() - 1;
                        }
                    }
                    if (selectedImages.isEmpty()) return;
                    if (lastAddedIndex >= 0) selectedEditIndex = lastAddedIndex;
                    ensureSelectedEditIndex();
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
                    if (selectedImages.isEmpty()) {
                        selectedImages.add(edited);
                        selectedEditIndex = 0;
                    } else {
                        ensureSelectedEditIndex();
                        selectedImages.set(selectedEditIndex, edited);
                    }
                    selectedImageUri = selectedImages.get(selectedEditIndex);
                    showSelectedImage();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (posting) {
                    Toast.makeText(CreatePostActivity.this,
                            "Ảnh đang được tải lên, vui lòng đợi.", Toast.LENGTH_SHORT).show();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        postService = PostService.getInstance();
        authService = AuthService.getInstance();
        authService.init(this);

        initViews();
        loadCurrentAvatar();
        editingPostId = getIntent().getStringExtra(EXTRA_EDIT_POST_ID);
        if (editingPostId != null && !editingPostId.isEmpty()) {
            ((TextView) findViewById(R.id.tvCreateTitle)).setText("Sửa bộ ảnh");
            btnSubmitPost.setText("Lưu");
        }

        if (savedInstanceState != null) {
            editLoaded = savedInstanceState.getBoolean(KEY_EDIT_LOADED, false);
            selectedEditIndex = savedInstanceState.getInt(KEY_EDIT_IMAGE_INDEX, -1);
            ArrayList<String> savedUris = savedInstanceState.getStringArrayList(KEY_IMAGE_URIS);
            if (savedUris != null) {
                for (String savedUri : savedUris) selectedImages.add(Uri.parse(savedUri));
            }
            String uriString = savedInstanceState.getString(KEY_IMAGE_URI);

            if (!selectedImages.isEmpty()) {
                ensureSelectedEditIndex();
                showSelectedImage();
            } else if (uriString != null && !uriString.isEmpty()) {
                selectedImageUri = Uri.parse(uriString);
                selectedImages.add(selectedImageUri);
                selectedEditIndex = 0;
                showSelectedImage();
            }
        }
        if (editingPostId != null && !editingPostId.isEmpty() && !editLoaded) {
            loadPostForEditing();
        }
    }

    private void loadPostForEditing() {
        btnSubmitPost.setEnabled(false);
        postService.getPostDetail(editingPostId, new PostService.Callback<Post>() {
            @Override public void onSuccess(Post post) {
                if (isFinishing() || isDestroyed()) return;
                String viewerId = authService.getCurrentUserId();
                if (viewerId == null || !viewerId.equals(post.getAuthorId())) {
                    Toast.makeText(CreatePostActivity.this, "Bạn không thể sửa ảnh của người khác.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                selectedImages.clear();
                for (String url : post.getImageUrls()) selectedImages.add(Uri.parse(url));
                selectedEditIndex = selectedImages.isEmpty() ? -1 : selectedImages.size() - 1;
                etCaption.setText(post.getCaption());
                StringBuilder tags = new StringBuilder();
                for (String tag : post.getHashtags()) {
                    if (tags.length() > 0) tags.append(' ');
                    tags.append(tag);
                }
                etHashtags.setText(tags.toString());
                editLoaded = true;
                showSelectedImage();
                btnSubmitPost.setEnabled(true);
            }

            @Override public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(CreatePostActivity.this,
                        "Không thể mở bộ ảnh để sửa: " + error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
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
        photoCollage = findViewById(R.id.photoCollage);
        llImagePlaceholder = findViewById(R.id.llImagePlaceholder);
        etCaption = findViewById(R.id.etCaption);
        etHashtags = findViewById(R.id.etHashtags);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnEditSelectedImage = findViewById(R.id.btnEditSelectedImage);
        btnAddMoreImages = findViewById(R.id.btnAddMoreImages);
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
                openImageSource();
            }
        });

        btnAddMoreImages.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openImageSource(); }
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
                ensureSelectedEditIndex();
                Uri imageToEdit = selectedImages.get(selectedEditIndex);
                String scheme = imageToEdit.getScheme();
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                    Toast.makeText(CreatePostActivity.this,
                            "Hãy thay ảnh đã chọn bằng ảnh trên máy trước khi chỉnh màu hoặc cắt.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(CreatePostActivity.this, PhotoEditorActivity.class);
                intent.putExtra(PhotoEditorActivity.EXTRA_IMAGE_URI, imageToEdit.toString());
                editorLauncher.launch(intent);
            }
        });

    }

    private void openImageSource() {
        if (posting || selectedImages.size() >= 10) return;
        ImagePickerDialog.show(this, new Runnable() {
            @Override public void run() { multiPickerLauncher.launch(new String[]{"image/*"}); }
        }, new Consumer<Uri>() {
            @Override public void accept(Uri uri) {
                if (selectedImages.contains(uri)) {
                    Toast.makeText(CreatePostActivity.this, "Ảnh này đã được chọn.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedImages.size() >= 10) return;
                selectedImages.add(uri);
                selectedEditIndex = selectedImages.size() - 1;
                selectedImageUri = selectedImages.get(selectedEditIndex);
                showSelectedImage();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout group(int orientation) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(orientation);
        return layout;
    }

    private void addCell(LinearLayout parent, View child) {
        LinearLayout.LayoutParams params = parent.getOrientation() == LinearLayout.HORIZONTAL
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        parent.addView(child, params);
    }

    private FrameLayout photoTile(int position, boolean overflow) {
        FrameLayout tile = new FrameLayout(this);
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Glide.with(this).load(selectedImages.get(position)).centerCrop().into(image);
        if (!overflow) {
            tile.setContentDescription("Chọn ảnh " + (position + 1) + " để chỉnh");
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    selectedEditIndex = position;
                    selectedImageUri = selectedImages.get(position);
                    updateEditButtonAccessibility();
                }
            });
        }

        if (overflow) {
            View shade = new View(this);
            shade.setBackgroundColor(0xAA10243B);
            tile.addView(shade, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView more = new TextView(this);
            more.setGravity(Gravity.CENTER);
            more.setText("+" + (selectedImages.size() - 3));
            more.setTextColor(Color.WHITE);
            more.setTextSize(28);
            more.setTypeface(null, android.graphics.Typeface.BOLD);
            tile.addView(more, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tile.setContentDescription("Xem và quản lý " + selectedImages.size() + " ảnh đã chọn");
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { showRemovePhotoDialog(); }
            });
        } else {
            FrameLayout removeHit = new FrameLayout(this);
            FrameLayout.LayoutParams hitParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
            hitParams.setMargins(0, dp(4), dp(4), 0);
            tile.addView(removeHit, hitParams);
            ImageView removeIcon = new ImageView(this);
            removeIcon.setImageResource(R.drawable.ic_close_white_24);
            removeIcon.setBackgroundResource(R.drawable.bg_photo_remove);
            removeIcon.setPadding(dp(5), dp(5), dp(5), dp(5));
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER);
            removeHit.addView(removeIcon, iconParams);
            removeHit.setContentDescription("Bỏ ảnh " + (position + 1));
            removeHit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { removePhoto(position); }
            });
        }
        return tile;
    }

    private void removePhoto(int position) {
        if (posting || position < 0 || position >= selectedImages.size()) return;
        int previousTarget = selectedEditIndex;
        selectedImages.remove(position);
        if (selectedImages.isEmpty()) {
            selectedEditIndex = -1;
            selectedImageUri = null;
        } else if (position == previousTarget) {
            selectedEditIndex = Math.max(0, position - 1);
            selectedImageUri = selectedImages.get(selectedEditIndex);
        } else {
            if (position < previousTarget) selectedEditIndex = previousTarget - 1;
            ensureSelectedEditIndex();
        }
        showSelectedImage();
    }

    private void ensureSelectedEditIndex() {
        if (selectedImages.isEmpty()) {
            selectedEditIndex = -1;
            selectedImageUri = null;
            return;
        }
        if (selectedEditIndex < 0 || selectedEditIndex >= selectedImages.size()) {
            selectedEditIndex = selectedImages.size() - 1;
        }
        selectedImageUri = selectedImages.get(selectedEditIndex);
    }

    private void updateEditButtonAccessibility() {
        if (selectedEditIndex >= 0 && selectedEditIndex < selectedImages.size()) {
            btnEditSelectedImage.setContentDescription("Chỉnh ảnh " + (selectedEditIndex + 1));
        } else {
            btnEditSelectedImage.setContentDescription("Chỉnh ảnh");
        }
    }

    private void showRemovePhotoDialog() {
        if (posting) return;
        String[] actions = new String[selectedImages.size()];
        for (int index = 0; index < actions.length; index++) {
            actions[index] = "Bỏ ảnh " + (index + 1);
        }
        new AlertDialog.Builder(this).setTitle("Chọn ảnh cần bỏ")
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { removePhoto(which); }
                }).setNegativeButton("Đóng", null).show();
    }

    private void showSelectedImage() {
        photoCollage.removeAllViews();
        int count = selectedImages.size();
        if (count == 0) {
            llImagePlaceholder.setVisibility(View.VISIBLE);
            photoCollage.setVisibility(View.GONE);
            tvSelectedCount.setText("Chọn tối đa 10 ảnh");
            btnAddMoreImages.setVisibility(View.GONE);
            btnEditSelectedImage.setVisibility(View.GONE);
        } else {
            ensureSelectedEditIndex();
            llImagePlaceholder.setVisibility(View.GONE);
            photoCollage.setVisibility(View.VISIBLE);
            photoCollage.setOrientation(count >= 4 ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            if (count == 1) {
                addCell(photoCollage, photoTile(0, false));
            } else if (count == 2) {
                addCell(photoCollage, photoTile(0, false));
                addCell(photoCollage, photoTile(1, false));
            } else if (count == 3) {
                addCell(photoCollage, photoTile(0, false));
                LinearLayout right = group(LinearLayout.VERTICAL);
                addCell(right, photoTile(1, false));
                addCell(right, photoTile(2, false));
                addCell(photoCollage, right);
            } else {
                LinearLayout top = group(LinearLayout.HORIZONTAL);
                LinearLayout bottom = group(LinearLayout.HORIZONTAL);
                addCell(top, photoTile(0, false));
                addCell(top, photoTile(1, false));
                addCell(bottom, photoTile(2, false));
                addCell(bottom, photoTile(3, true));
                addCell(photoCollage, top);
                addCell(photoCollage, bottom);
            }
            tvSelectedCount.setText(count + " ảnh đã chọn" + (count >= 4 ? " · Nhấn +" + (count - 3) + " để quản lý" : ""));
            btnAddMoreImages.setText(count == 1 ? "＋ Thêm ảnh thứ 2" : "＋ Thêm ảnh");
            btnAddMoreImages.setVisibility(count < 10 ? View.VISIBLE : View.GONE);
            btnEditSelectedImage.setVisibility(View.VISIBLE);
            updateEditButtonAccessibility();
        }
        ViewGroup.LayoutParams frameParams = btnSelectImage.getLayoutParams();
        frameParams.height = dp(count >= 4 ? 320 : 260);
        btnSelectImage.setLayoutParams(frameParams);
    }

    private void handleCreatePost() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(
                    this,
                    "Vui lòng chọn ít nhất một ảnh.",
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
        btnAddMoreImages.setEnabled(false);
        btnEditSelectedImage.setEnabled(false);
        btnBackCreate.setEnabled(false);
        llLoadingCreate.setVisibility(View.VISIBLE);
        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText(editingPostId == null ? "Đang chia sẻ" : "Đang lưu");

        List<String> hashtags = HashtagParser.extract(
                caption, etHashtags.getText().toString());
        PostService.Callback<Post> saveCallback = new PostService.Callback<Post>() {
            @Override
            public void onSuccess(Post post) {
                posting = false;
                llLoadingCreate.setVisibility(View.GONE);
                btnSubmitPost.setEnabled(true);
                btnSelectImage.setEnabled(true);
                btnAddMoreImages.setEnabled(true);
                btnEditSelectedImage.setEnabled(true);

                Toast.makeText(CreatePostActivity.this,
                        editingPostId == null ? "Đã chia sẻ ảnh!" : "Đã lưu bộ ảnh!",
                        Toast.LENGTH_SHORT).show();

                if (editingPostId != null) {
                    getSharedPreferences("dlog_ui", MODE_PRIVATE).edit()
                            .putBoolean("refresh_feed_after_edit", true).apply();
                    Intent home = new Intent(CreatePostActivity.this, HomeActivity.class);
                    home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    home.putExtra("refresh_after_post", true);
                    startActivity(home);
                    finish();
                    return;
                }

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
                btnSubmitPost.setText(editingPostId == null ? "Chia sẻ" : "Lưu");
                btnSelectImage.setEnabled(true);
                btnAddMoreImages.setEnabled(true);
                btnEditSelectedImage.setEnabled(true);
                btnBackCreate.setEnabled(true);

                String message;

                if (e == null || e.getMessage() == null) {
                    message = "Không thể đăng bài. Vui lòng thử lại.";
                } else {
                    message = e.getMessage();
                }

                Toast.makeText(
                        CreatePostActivity.this,
                        "Không thể chia sẻ ảnh: " + message,
                        Toast.LENGTH_LONG
                ).show();
            }
        };
        if (editingPostId == null) {
            postService.createPost(currentUserId, caption, new ArrayList<Uri>(selectedImages),
                    hashtags, this, saveCallback);
        } else {
            postService.updatePostWithImages(currentUserId, editingPostId, caption,
                    new ArrayList<Uri>(selectedImages), hashtags, this, saveCallback);
        }
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
        outState.putBoolean(KEY_EDIT_LOADED, editLoaded);
        outState.putInt(KEY_EDIT_IMAGE_INDEX, selectedEditIndex);
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
