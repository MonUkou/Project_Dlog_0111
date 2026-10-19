package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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

/**
 * Activity for editing user profile information including username, full name, bio, and avatar.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

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

    private final ActivityResultLauncher<Intent> avatarPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    newAvatarUri = result.getData().getData();
                    Log.d(TAG, "New avatar selected: " + newAvatarUri);
                    updateAvatarImagePreview(newAvatarUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userService = UserService.getInstance();
        authService = AuthService.getInstance();

        initViews();
        setupListeners();
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
    }

    private void setupListeners() {
        btnBackEditProfile.setOnClickListener(v -> finish());
        btnChangeAvatar.setOnClickListener(v -> openGalleryForAvatar());
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());
    }

    private void openGalleryForAvatar() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        avatarPickerLauncher.launch(intent);
    }

    private void updateAvatarImagePreview(Object source) {
        if (source != null) {
            Glide.with(this)
                    .load(source)
                    .circleCrop()
                    .into(ivEditAvatar);
        }
    }

    private void loadCurrentProfile() {
        String uid = authService.getCurrentUserId();
        Log.d(TAG, "Loading current user profile for UID: " + uid);

        userService.getUserProfile(uid, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUserModel = user;
                if (user != null) {
                    populateUserProfileFields(user);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user profile", e);
            }
        });
    }

    private void populateUserProfileFields(User user) {
        etEditUsername.setText(user.getUsername());
        etEditFullName.setText(user.getFullName());
        etEditBio.setText(user.getBio());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            updateAvatarImagePreview(user.getAvatarUrl());
        }
    }

    private void handleSaveProfile() {
        String newUsername = etEditUsername.getText().toString().trim();
        String newFullName = etEditFullName.getText().toString().trim();
        String newBio = etEditBio.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Tên người dùng không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving profile modifications...");
        setLoadingState(true);

        if (currentUserModel == null) {
            currentUserModel = new User();
            currentUserModel.setId(authService.getCurrentUserId());
        }

        currentUserModel.setUsername(newUsername);
        currentUserModel.setFullName(newFullName);
        currentUserModel.setBio(newBio);

        if (newAvatarUri != null) {
            uploadAvatarAndSave();
        } else {
            saveUserToFirestore();
        }
    }

    private void uploadAvatarAndSave() {
        // Upload new avatar to Firebase Storage
        StorageReference avatarRef = FirebaseStorage.getInstance().getReference()
                .child("avatars/" + currentUserModel.getId() + "_" + System.currentTimeMillis() + ".jpg");

        avatarRef.putFile(newAvatarUri)
                .addOnSuccessListener(taskSnapshot -> avatarRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "Avatar uploaded successfully: " + downloadUri);
                            currentUserModel.setAvatarUrl(downloadUri.toString());
                            saveUserToFirestore();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL", e);
                            currentUserModel.setAvatarUrl(newAvatarUri.toString());
                            saveUserToFirestore();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload avatar image", e);
                    currentUserModel.setAvatarUrl(newAvatarUri.toString());
                    saveUserToFirestore();
                });
    }

    private void saveUserToFirestore() {
        userService.updateUserProfile(currentUserModel, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoadingState(false);
                Log.i(TAG, "User profile saved successfully");
                Toast.makeText(EditProfileActivity.this, "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                setLoadingState(false);
                Log.e(TAG, "Error updating profile: " + e.getMessage(), e);
                Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        progressBarEditProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
    }
}
