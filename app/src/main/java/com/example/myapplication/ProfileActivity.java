package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvHeaderUsername, tvDisplayName, tvBio;
    private TextView tvPostsCount, tvFollowersCount, tvFollowingCount;
    private Button btnAction;
    private ProgressBar progressBar;

    private UserService userService;
    private AuthService authService;
    private String profileUserId;
    private String currentUserId;
    private boolean isMyProfile = true;
    private boolean isFollowing = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvBio = findViewById(R.id.tvBio);
        tvPostsCount = findViewById(R.id.tvPostsCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        btnAction = findViewById(R.id.btnAction);
        progressBar = findViewById(R.id.progressBar);

        userService = new UserService();
        authService = new AuthService();

        if (!authService.isLoggedIn()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = authService.getCurrentUserId();
        profileUserId = getIntent().getStringExtra("user_id");
        if (profileUserId == null || profileUserId.trim().isEmpty()) {
            profileUserId = currentUserId;
        }

        isMyProfile = profileUserId.equals(currentUserId);

        if (isMyProfile) {
            btnAction.setText("Sửa hồ sơ");
            btnAction.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            });
        } else {
            checkFollowStatus();
        }

        // Setup bottom navigation bar
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, SearchActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {});
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                User user = userService.getUserProfile(profileUserId);
                boolean following = false;
                if (!isMyProfile) {
                    following = userService.isFollowing(currentUserId, profileUserId);
                }
                boolean finalFollowing = following;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (user != null) {
                        tvHeaderUsername.setText(user.getUsername() != null ? user.getUsername() : "profile");
                        tvDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
                        tvBio.setText(user.getBio() != null ? user.getBio() : "");
                        tvPostsCount.setText(String.valueOf(user.getPostsCount()));
                        tvFollowersCount.setText(String.valueOf(user.getFollowerCount()));
                        tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));

                        if (!isMyProfile) {
                            isFollowing = finalFollowing;
                            btnAction.setText(isFollowing ? "Đang theo dõi" : "Theo dõi");
                            btnAction.setOnClickListener(v -> toggleFollow());
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkFollowStatus() {
        executorService.execute(() -> {
            boolean following = userService.isFollowing(currentUserId, profileUserId);
            runOnUiThread(() -> {
                isFollowing = following;
                btnAction.setText(isFollowing ? "Đang theo dõi" : "Theo dõi");
                btnAction.setOnClickListener(v -> toggleFollow());
            });
        });
    }

    private void toggleFollow() {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                if (isFollowing) {
                    userService.unfollowUser(currentUserId, profileUserId);
                    isFollowing = false;
                } else {
                    userService.followUser(currentUserId, profileUserId);
                    isFollowing = true;
                }
                User updatedUser = userService.getUserProfile(profileUserId);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAction.setText(isFollowing ? "Đang theo dõi" : "Theo dõi");
                    if (updatedUser != null) {
                        tvFollowersCount.setText(String.valueOf(updatedUser.getFollowerCount()));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
