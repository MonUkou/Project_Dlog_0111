package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.util.ImagePickerDialog;

public class EditProfileActivity extends AppCompatActivity {
    private Button save;
    private ImageView avatar, cover;
    private EditText username, fullName, bio;
    private ProgressBar progress;
    private User user;
    private Uri avatarUri, coverUri;
    private boolean pickingCover, saving;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), new ActivityResultCallback<Uri>() {
            @Override public void onActivityResult(Uri uri) {
                if (uri == null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                select(uri);
            }
        });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_edit_profile);
        if (AuthService.getInstance().getCurrentUserId() == null) { finish(); return; }
        save = findViewById(R.id.btnSaveProfile);
        avatar = findViewById(R.id.ivEditAvatar); cover = findViewById(R.id.ivEditCover);
        username = findViewById(R.id.etEditUsername); fullName = findViewById(R.id.etEditFullName);
        bio = findViewById(R.id.etEditBio); progress = findViewById(R.id.progressBarEditProfile);
        if (state != null) {
            pickingCover = state.getBoolean("pickingCover");
            if (state.getString("avatarUri") != null) avatarUri = Uri.parse(state.getString("avatarUri"));
            if (state.getString("coverUri") != null) coverUri = Uri.parse(state.getString("coverUri"));
        }
        findViewById(R.id.btnBackEditProfile).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { if (!saving) finish(); }
        });
        findViewById(R.id.btnChangeAvatar).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { choose(false); }
        });
        findViewById(R.id.btnChangeCover).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { choose(true); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveProfile(); }
        });
        save.setEnabled(false); progress.setVisibility(View.VISIBLE);
        UserService.getInstance().getUserProfile(AuthService.getInstance().getCurrentUserId(), new UserService.Callback<User>() {
            public void onSuccess(User result) {
                if (isDestroyed()) return;
                user = result;
                if (state == null) {
                    username.setText(user.getUsername()); fullName.setText(user.getFullName()); bio.setText(user.getBio());
                }
                preview(); progress.setVisibility(View.GONE); save.setEnabled(true);
                if (state == null) {
                    String requestedImage = getIntent().getStringExtra("pick_image_kind");
                    if ("avatar".equals(requestedImage) || "cover".equals(requestedImage)) {
                        getIntent().removeExtra("pick_image_kind");
                        choose("cover".equals(requestedImage));
                    }
                }
            }
            public void onError(Exception error) { fail(error); }
        });
    }
    private void choose(boolean isCover) {
        if (saving) return;
        pickingCover = isCover;
        ImagePickerDialog.show(this, new Runnable() {
            @Override public void run() {
                try { picker.launch(new String[]{"image/*"}); }
                catch (Exception error) { fail(error); }
            }
        }, new java.util.function.Consumer<Uri>() {
            @Override public void accept(Uri uri) { select(uri); }
        });
    }
    private void select(Uri uri) {
        if (pickingCover) coverUri = uri; else avatarUri = uri;
        preview();
    }
    private void preview() {
        Glide.with(this).load(avatarUri != null ? avatarUri : user == null ? null : user.getAvatarUrl())
            .placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
        Glide.with(this).load(coverUri != null ? coverUri : user == null ? null : user.getCoverUrl())
            .placeholder(R.drawable.profile_cover_sample).error(R.drawable.profile_cover_sample).centerCrop().into(cover);
    }
    private void busy(boolean value) {
        saving = value; save.setEnabled(!value && user != null);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        username.setEnabled(!value); fullName.setEnabled(!value); bio.setEnabled(!value);
        findViewById(R.id.btnChangeAvatar).setEnabled(!value);
        findViewById(R.id.btnChangeCover).setEnabled(!value);
    }
    private void saveProfile() {
        if (saving || user == null) return;
        String name = username.getText().toString().trim();
        if (name.isEmpty()) { username.setError("Vui lòng nhập tên người dùng"); username.requestFocus(); return; }
        user.setUsername(name); user.setFullName(fullName.getText().toString().trim());
        user.setBio(bio.getText().toString().trim()); busy(true);
        uploadAvatar();
    }
    private void uploadAvatar() {
        if (avatarUri == null) { uploadCover(); return; }
        PostService.getInstance().uploadProfileImage(user.getId(), "avatars", avatarUri, this, new PostService.Callback<String>() {
            public void onSuccess(String url) { user.setAvatarUrl(url); avatarUri = null; uploadCover(); }
            public void onError(Exception error) { fail(error); }
        });
    }
    private void uploadCover() {
        if (coverUri == null) { persist(); return; }
        PostService.getInstance().uploadProfileImage(user.getId(), "covers", coverUri, this, new PostService.Callback<String>() {
            public void onSuccess(String url) { user.setCoverUrl(url); coverUri = null; persist(); }
            public void onError(Exception error) { fail(error); }
        });
    }
    private void persist() {
        UserService.getInstance().updateUserProfile(user, new UserService.Callback<User>() {
            public void onSuccess(User result) {
                if (isDestroyed()) return;
                busy(false); Toast.makeText(EditProfileActivity.this, "Đã lưu hồ sơ lên Firebase", Toast.LENGTH_SHORT).show(); finish();
            }
            public void onError(Exception error) { fail(error); }
        });
    }
    private void fail(Exception error) {
        if (isDestroyed()) return;
        busy(false);
        Toast.makeText(this, "Không thể lưu/tải hồ sơ. Vui lòng thử lại: " + error.getMessage(), Toast.LENGTH_LONG).show();
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("pickingCover", pickingCover);
        if (avatarUri != null) state.putString("avatarUri", avatarUri.toString());
        if (coverUri != null) state.putString("coverUri", coverUri.toString());
    }
}
