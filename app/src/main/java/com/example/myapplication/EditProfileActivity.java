package com.example.myapplication;

import android.content.Intent;
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

import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatarEdit;
    private EditText etDisplayName, etBio;
    private ProgressBar progressBarEdit;
    private TextView tvCancel, tvSaveTop;
    private Button btnLogout;

    private UserService userService;
    private AuthService authService;
    private String currentUserId;
    private Uri selectedImageUri;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivAvatarEdit.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ivAvatarEdit = findViewById(R.id.ivAvatarEdit);
        TextView btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        tvCancel = findViewById(R.id.tvCancel);
        tvSaveTop = findViewById(R.id.tvSaveTop);
        btnLogout = findViewById(R.id.btnLogout);
        etDisplayName = findViewById(R.id.etDisplayName);
        etBio = findViewById(R.id.etBio);
        progressBarEdit = findViewById(R.id.progressBarEdit);

        userService = new UserService();
        authService = new AuthService();

        if (!authService.isLoggedIn()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = authService.getCurrentUserId();
        loadCurrentUserData();

        btnChangeAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        tvCancel.setOnClickListener(v -> finish());
        tvSaveTop.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> {
            authService.logout();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadCurrentUserData() {
        progressBarEdit.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                User user = userService.getUserProfile(currentUserId);
                runOnUiThread(() -> {
                    progressBarEdit.setVisibility(View.GONE);
                    if (user != null) {
                        etDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
                        etBio.setText(user.getBio() != null ? user.getBio() : "");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarEdit.setVisibility(View.GONE);
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveProfile() {
        String displayName = etDisplayName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (displayName.isEmpty()) {
            etDisplayName.setError("Display name cannot be empty");
            return;
        }

        progressBarEdit.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                String avatarUrl = null;
                if (selectedImageUri != null) {
                    avatarUrl = userService.uploadAvatar(currentUserId, selectedImageUri);
                } else {
                    User existing = userService.getUserProfile(currentUserId);
                    if (existing != null) {
                        avatarUrl = existing.getAvatarUrl();
                    }
                }

                userService.updateProfile(currentUserId, displayName, bio, avatarUrl);

                runOnUiThread(() -> {
                    progressBarEdit.setVisibility(View.GONE);
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarEdit.setVisibility(View.GONE);
                    Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
