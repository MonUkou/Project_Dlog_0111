package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBackEditProfile;
    private Button btnSaveProfile;
    private ImageView ivEditAvatar;
    private TextView btnChangeAvatar;
    private EditText etEditUsername;
    private EditText etEditFullName;
    private EditText etEditBio;
    private ProgressBar progressBarEditProfile;

    private UserService userService;
    private AuthService authService;

    private Uri newAvatarUri;
    private User currentUserModel;

    private final ActivityResultLauncher<String> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    newAvatarUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(ivEditAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userService = UserService.getInstance();
        authService = AuthService.getInstance();

        initViews();
        loadCurrentProfile();
    }

    private void initViews() {
        btnBackEditProfile = findViewById(R.id.btnBackEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        etEditUsername = findViewById(R.id.etEditUsername);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditBio = findViewById(R.id.etEditBio);
        progressBarEditProfile = findViewById(R.id.progressBarEditProfile);

        btnBackEditProfile.setOnClickListener(v -> finish());
        btnChangeAvatar.setOnClickListener(v -> openGalleryForAvatar());
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());
    }

    private void openGalleryForAvatar() {
        avatarPickerLauncher.launch("image/*");
    }

    private void loadCurrentProfile() {
        String uid = authService.getCurrentUserId();
        userService.getUserProfile(uid, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUserModel = user;
                if (user != null) {
                    etEditUsername.setText(user.getUsername());
                    etEditFullName.setText(user.getFullName());
                    etEditBio.setText(user.getBio());

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                                .load(user.getAvatarUrl())
                                .circleCrop()
                                .into(ivEditAvatar);
                    }
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void handleSaveProfile() {
        String newUsername = etEditUsername.getText().toString().trim();
        String newFullName = etEditFullName.getText().toString().trim();
        String newBio = etEditBio.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Tên người dùng không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarEditProfile.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        if (currentUserModel == null) {
            currentUserModel = new User();
            currentUserModel.setId(authService.getCurrentUserId());
        }

        currentUserModel.setUsername(newUsername);
        currentUserModel.setFullName(newFullName);
        currentUserModel.setBio(newBio);

        if (newAvatarUri != null) {
            // Upload new avatar to Firebase Storage
            StorageReference avatarRef = FirebaseStorage.getInstance().getReference()
                    .child("avatars/" + currentUserModel.getId() + "_" + System.currentTimeMillis() + ".jpg");

            avatarRef.putFile(newAvatarUri)
                    .addOnSuccessListener(taskSnapshot -> avatarRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                currentUserModel.setAvatarUrl(downloadUri.toString());
                                saveUserToFirestore();
                            })
                            .addOnFailureListener(this::showAvatarUploadError))
                    .addOnFailureListener(this::showAvatarUploadError);
        } else {
            saveUserToFirestore();
        }
    }

    private void saveUserToFirestore() {
        userService.updateUserProfile(currentUserModel, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                progressBarEditProfile.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                progressBarEditProfile.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAvatarUploadError(Exception error) {
        progressBarEditProfile.setVisibility(View.GONE);
        btnSaveProfile.setEnabled(true);
        Toast.makeText(this, "Không tải được ảnh đại diện lên Firebase: " + error.getMessage(),
                Toast.LENGTH_LONG).show();
    }
}
