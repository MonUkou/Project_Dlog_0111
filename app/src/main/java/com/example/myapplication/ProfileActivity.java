package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.PostGridAdapter;
import com.example.myapplication.model.Follow;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.NotificationService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileTitleUsername;
    private ImageView ivProfileAvatar;
    private TextView tvPostsCount;
    private TextView tvFollowersCount;
    private TextView tvFollowingCount;
    private TextView tvFullName;
    private TextView tvBio;
    private Button btnProfileActionButton;

    private LinearLayout llProfileTabs;
    private TextView tabUserPosts;
    private TextView tabSavedPosts;

    private RecyclerView rvProfilePosts;
    private ProgressBar progressBarProfile;
    private TextView tvStateProfile;

    private PostGridAdapter postGridAdapter;
    private UserService userService;
    private PostService postService;
    private AuthService authService;
    private NotificationService notificationService;

    private String targetUserId;
    private String currentUserId;
    private boolean isSelfProfile;
    private boolean isShowingSavedPosts = false;
    private boolean isFollowingTarget = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userService = UserService.getInstance();
        postService = PostService.getInstance();
        authService = AuthService.getInstance();
        notificationService = NotificationService.getInstance();

        currentUserId = authService.getCurrentUserId();
        targetUserId = getIntent().getStringExtra("userId");
        if (targetUserId == null || targetUserId.isEmpty()) {
            targetUserId = currentUserId;
        }

        isSelfProfile = targetUserId.equals(currentUserId);

        initViews();
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);

        loadUserProfile();
        loadUserPosts();
    }

    private void initViews() {
        tvProfileTitleUsername = findViewById(R.id.tvProfileTitleUsername);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvPostsCount = findViewById(R.id.tvPostsCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        btnProfileActionButton = findViewById(R.id.btnProfileActionButton);

        llProfileTabs = findViewById(R.id.llProfileTabs);
        tabUserPosts = findViewById(R.id.tabUserPosts);
        tabSavedPosts = findViewById(R.id.tabSavedPosts);

        rvProfilePosts = findViewById(R.id.rvProfilePosts);
        progressBarProfile = findViewById(R.id.progressBarProfile);
        tvStateProfile = findViewById(R.id.tvStateProfile);

        if (isSelfProfile) {
            btnProfileActionButton.setText("Sửa hồ sơ");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_button_outline);
            btnProfileActionButton.setTextColor(0xFF000000);

            llProfileTabs.setVisibility(View.VISIBLE);
            tabUserPosts.setOnClickListener(v -> switchTab(false));
            tabSavedPosts.setOnClickListener(v -> switchTab(true));
        } else {
            llProfileTabs.setVisibility(View.GONE);
            checkFollowStatus();
        }

        btnProfileActionButton.setOnClickListener(v -> {
            if (isSelfProfile) {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            } else {
                handleFollowToggle();
            }
        });
    }

    private void setupRecyclerView() {
        rvProfilePosts.setLayoutManager(new GridLayoutManager(this, 3));
        postGridAdapter = new PostGridAdapter(this);
        rvProfilePosts.setAdapter(postGridAdapter);
    }

    private void switchTab(boolean showSaved) {
        if (isShowingSavedPosts == showSaved) return;
        isShowingSavedPosts = showSaved;

        if (showSaved) {
            tabUserPosts.setTextColor(0xFF888888);
            tabUserPosts.setTypeface(null, Typeface.NORMAL);

            tabSavedPosts.setTextColor(0xFF000000);
            tabSavedPosts.setTypeface(null, Typeface.BOLD);

            loadSavedPosts();
        } else {
            tabSavedPosts.setTextColor(0xFF888888);
            tabSavedPosts.setTypeface(null, Typeface.NORMAL);

            tabUserPosts.setTextColor(0xFF000000);
            tabUserPosts.setTypeface(null, Typeface.BOLD);

            loadUserPosts();
        }
    }

    private void loadUserProfile() {
        userService.getUserProfile(targetUserId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    tvProfileTitleUsername.setText(user.getUsername() != null ? user.getUsername() : user.getId());
                    tvFullName.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "Tên hiển thị");
                    tvBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Giới thiệu ngắn về bản thân.");

                    tvPostsCount.setText(String.valueOf(user.getPostsCount()));
                    tvFollowersCount.setText(String.valueOf(user.getFollowersCount()));
                    tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.getAvatarUrl())
                                .circleCrop()
                                .into(ivProfileAvatar);
                    } else {
                        ivProfileAvatar.setImageResource(R.drawable.bg_circle_avatar);
                    }
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void checkFollowStatus() {
        userService.isFollowing(currentUserId, targetUserId, new UserService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFollowing) {
                isFollowingTarget = isFollowing;
                updateFollowButtonUI();
            }

            @Override
            public void onError(Exception e) {
                isFollowingTarget = false;
                updateFollowButtonUI();
            }
        });
    }

    private void updateFollowButtonUI() {
        if (isFollowingTarget) {
            btnProfileActionButton.setText("Đang theo dõi");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_button_outline);
            btnProfileActionButton.setTextColor(0xFF000000);
        } else {
            btnProfileActionButton.setText("Theo dõi");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_button_black);
            btnProfileActionButton.setTextColor(0xFFFFFFFF);
        }
    }

    private void handleFollowToggle() {
        if (isFollowingTarget) {
            userService.unfollowUser(currentUserId, targetUserId, new UserService.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isFollowingTarget = false;
                    updateFollowButtonUI();
                    loadUserProfile();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(ProfileActivity.this, "Lỗi bỏ theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            userService.followUser(currentUserId, targetUserId, new UserService.Callback<Follow>() {
                @Override
                public void onSuccess(Follow result) {
                    isFollowingTarget = true;
                    updateFollowButtonUI();
                    loadUserProfile();

                    notificationService.createNotificationAsync(targetUserId, currentUserId, currentUserId, "FOLLOW", "");
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(ProfileActivity.this, "Lỗi theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadUserPosts() {
        progressBarProfile.setVisibility(View.VISIBLE);
        tvStateProfile.setVisibility(View.GONE);

        postService.getPostsByUserId(targetUserId, new PostService.Callback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                progressBarProfile.setVisibility(View.GONE);
                tvPostsCount.setText(String.valueOf(posts.size()));

                if (posts.isEmpty()) {
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa có bài viết nào");
                } else {
                    tvStateProfile.setVisibility(View.GONE);
                }
                postGridAdapter.setPosts(posts);
            }

            @Override
            public void onError(Exception e) {
                progressBarProfile.setVisibility(View.GONE);
                tvStateProfile.setVisibility(View.VISIBLE);
                tvStateProfile.setText("Không thể tải bài viết");
            }
        });
    }

    private void loadSavedPosts() {
        progressBarProfile.setVisibility(View.VISIBLE);
        tvStateProfile.setVisibility(View.GONE);

        postService.getSavedPosts(targetUserId, new PostService.Callback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                progressBarProfile.setVisibility(View.GONE);
                if (posts.isEmpty()) {
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa có bài viết đã lưu");
                } else {
                    tvStateProfile.setVisibility(View.GONE);
                }
                postGridAdapter.setPosts(posts);
            }

            @Override
            public void onError(Exception e) {
                progressBarProfile.setVisibility(View.GONE);
                tvStateProfile.setVisibility(View.VISIBLE);
                tvStateProfile.setText("Không thể tải bài viết đã lưu");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);
        loadUserProfile();
        if (isShowingSavedPosts) {
            loadSavedPosts();
        } else {
            loadUserPosts();
        }
    }
}
