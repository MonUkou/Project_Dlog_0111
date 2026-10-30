package com.example.myapplication;

import android.content.Intent;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.PostGridAdapter;
import com.example.myapplication.adapter.PostAdapter;
import com.example.myapplication.model.Follow;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.ChatService;
import com.example.myapplication.service.impl.NotificationService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProfileActivity extends AppCompatActivity {

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
    private TextView tvOfflineProfile;

    private PostGridAdapter postGridAdapter;
    private List<Post> displayedPosts = new ArrayList<>();
    private int sortMode;
    private TextView sortNewest, sortOldest, sortPopular;
    private View profileSortBar;
    private UserService userService;
    private PostService postService;
    private AuthService authService;
    private NotificationService notificationService;

    private String targetUserId;
    private String currentUserId;
    private boolean isSelfProfile;
    public boolean isSelfProfile() { return isSelfProfile; }
    private boolean isShowingSavedPosts = false;
    private boolean isFollowingTarget = false;
    private ListenerRegistration profileRegistration;

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
        if (getIntent().getBooleanExtra("openOwnProfile", false)) targetUserId = currentUserId;
        if (targetUserId == null || targetUserId.isEmpty()) {
            targetUserId = currentUserId;
        }

        if (targetUserId == null || targetUserId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); return;
        }
        isSelfProfile = targetUserId.equals(currentUserId);

        com.example.myapplication.service.impl.OfflinePages offlinePages =
                new com.example.myapplication.service.impl.OfflinePages(this);
        if (!com.example.myapplication.util.NetworkState.isOnline(this)
                && !offlinePages.hasProfile(targetUserId)) {
            Toast.makeText(this,
                    "Hồ sơ này chưa được mở trong phiên khi có mạng.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);

        if (getIntent().getBooleanExtra("showSavedPosts", false)) {
            switchTab(true);
        }

        loadUserProfile();
        loadUserPosts();
    }

    private void initViews() {
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        ivProfileAvatar.setImageResource(R.drawable.bg_circle_avatar);
        tvPostsCount = findViewById(R.id.tvPostsCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        btnProfileActionButton = findViewById(R.id.btnProfileActionButton);
        btnProfileActionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dlog_button)));
        btnProfileActionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        findViewById(R.id.btnShowFollowers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFollowList(false);
            }
        });
        findViewById(R.id.btnShowFollowing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFollowList(true);
            }
        });

        llProfileTabs = findViewById(R.id.llProfileTabs);
        tabUserPosts = findViewById(R.id.tabUserPosts);
        tabSavedPosts = findViewById(R.id.tabSavedPosts);

        rvProfilePosts = findViewById(R.id.rvProfilePosts);
        progressBarProfile = findViewById(R.id.progressBarProfile);
        tvStateProfile = findViewById(R.id.tvStateProfile);
        tvOfflineProfile = findViewById(R.id.tvOfflineProfile);
        profileSortBar = findViewById(R.id.profileSortBar);
        sortNewest = findViewById(R.id.profileSortNewest);
        sortOldest = findViewById(R.id.profileSortOldest);
        sortPopular = findViewById(R.id.profileSortPopular);
        sortNewest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setSortMode(0); }
        });
        sortOldest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setSortMode(1); }
        });
        sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setSortMode(2); }
        });
        updateSortControls();

        findViewById(R.id.btnProfileSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
            }
        });

        if (isSelfProfile) {
            findViewById(R.id.btnMessageProfile).setVisibility(View.GONE);
            findViewById(R.id.btnChangeAvatarProfile).setVisibility(View.VISIBLE);
            findViewById(R.id.btnChangeCoverProfile).setVisibility(View.VISIBLE);
            findViewById(R.id.btnChangeAvatarProfile).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { openImageEditor("avatar"); }
            });
            findViewById(R.id.btnChangeCoverProfile).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { openImageEditor("cover"); }
            });
            btnProfileActionButton.setText("Sửa hồ sơ");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_profile_action);
            btnProfileActionButton.setTextColor(ContextCompat.getColor(this, R.color.white));

            llProfileTabs.setVisibility(View.VISIBLE);
            tabUserPosts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(false);
                }
            });
            tabSavedPosts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(true);
                }
            });
        } else {
            findViewById(R.id.btnProfileSettings).setVisibility(View.GONE);
            llProfileTabs.setVisibility(View.GONE);
            findViewById(R.id.btnMessageProfile).setVisibility(View.VISIBLE);
            findViewById(R.id.btnMessageProfile).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPrivateChat();
                }
            });
            checkFollowStatus();
        }

        btnProfileActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSelfProfile) {
                    Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                    startActivity(intent);
                } else {
                    handleFollowToggle();
                }
            }
        });
    }

    private void openFollowList(boolean following) {
        Intent intent = new Intent(this, FollowListActivity.class);
        intent.putExtra("userId", targetUserId);
        intent.putExtra("following", following);
        startActivity(intent);
    }

    private void openImageEditor(String kind) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra("pick_image_kind", kind);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        rvProfilePosts.setLayoutManager(new GridLayoutManager(this, 3));
        postGridAdapter = new PostGridAdapter(this);
        postGridAdapter.setOnPostClickListener(this::showPostDialog);
        rvProfilePosts.setAdapter(postGridAdapter);
    }

    private void showPostDialog(Post post) {
        Dialog dialog = new Dialog(this);
        RecyclerView postList = new RecyclerView(this);
        postList.setLayoutManager(new LinearLayoutManager(this));
        PostAdapter adapter = new PostAdapter(this);
        postList.setAdapter(adapter);
        adapter.setPosts(Collections.singletonList(post));
        dialog.setContentView(postList);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.7f);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override public void onDismiss(android.content.DialogInterface ignored) {
                if (isShowingSavedPosts) loadSavedPosts();
            }
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(540));
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.84f);
            dialog.getWindow().setLayout(width, height);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setSortMode(int mode) {
        sortMode = mode;
        updateSortControls();
        showSortedPosts();
    }

    private void updateSortControls() {
        TextView[] controls = {sortNewest, sortOldest, sortPopular};
        for (int i = 0; i < controls.length; i++) {
            boolean selected = i == sortMode;
            controls[i].setSelected(selected);
            controls[i].setTextColor(ContextCompat.getColor(this,
                    selected ? R.color.dlog_blue : R.color.dlog_muted_blue));
        }
    }

    private void showSortedPosts() {
        List<Post> sorted = new ArrayList<>(displayedPosts);
        Collections.sort(sorted, new Comparator<Post>() {
            @Override public int compare(Post left, Post right) {
                if (sortMode == 2) {
                    int likes = Long.compare(right.getLikesCount(), left.getLikesCount());
                    if (likes != 0) return likes;
                }
                return sortMode == 1
                        ? Long.compare(left.getCreatedAt(), right.getCreatedAt())
                        : Long.compare(right.getCreatedAt(), left.getCreatedAt());
            }
        });
        postGridAdapter.setPosts(sorted);
    }

    private void displayPosts(List<Post> posts) {
        displayedPosts = posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        showSortedPosts();
    }

    private void switchTab(boolean showSaved) {
        if (isShowingSavedPosts == showSaved) return;
        isShowingSavedPosts = showSaved;
        profileSortBar.setVisibility(showSaved ? View.GONE : View.VISIBLE);

        if (showSaved) {
            tabUserPosts.setTextColor(ContextCompat.getColor(this, R.color.dlog_muted));
            tabUserPosts.setTypeface(ResourcesCompat.getFont(this, R.font.inter_regular));

            tabSavedPosts.setTextColor(ContextCompat.getColor(this, R.color.dlog_ink));
            tabSavedPosts.setTypeface(ResourcesCompat.getFont(this, R.font.inter_bold));

            loadSavedPosts();
        } else {
            tabSavedPosts.setTextColor(ContextCompat.getColor(this, R.color.dlog_muted));
            tabSavedPosts.setTypeface(ResourcesCompat.getFont(this, R.font.inter_regular));

            tabUserPosts.setTextColor(ContextCompat.getColor(this, R.color.dlog_ink));
            tabUserPosts.setTypeface(ResourcesCompat.getFont(this, R.font.inter_bold));

            loadUserPosts();
        }
    }

    private void loadUserProfile() {
        User cached = new com.example.myapplication.service.impl.OfflinePages(this).profile(targetUserId);
        if (cached != null) showUserProfile(cached);
        if (!com.example.myapplication.util.NetworkState.isOnline(this)) {
            if (cached == null) {
                tvStateProfile.setVisibility(View.VISIBLE);
                tvStateProfile.setText("Hồ sơ này chưa được mở trong phiên khi có mạng");
            }
            return;
        }
        if (profileRegistration != null) return;
        profileRegistration = userService.observeUserProfile(targetUserId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    new com.example.myapplication.service.impl.OfflinePages(ProfileActivity.this)
                            .saveProfile(targetUserId, user);
                    showUserProfile(user);
                }
            }

            @Override
            public void onError(Exception e) {
                if (cached != null) Toast.makeText(ProfileActivity.this, "Đang xem hồ sơ đã lưu trên máy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserProfile(User user) {
                    loadedTargetUser = user;
                    Glide.with(ProfileActivity.this).load(user.getCoverUrl())
                            .placeholder(R.drawable.profile_cover_sample).error(R.drawable.profile_cover_sample)
                            .centerCrop().into((ImageView)findViewById(R.id.ivProfileCover));
                    tvFullName.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "Tên hiển thị");
                    tvBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Giới thiệu ngắn về bản thân.");

                    tvPostsCount.setText(String.valueOf(user.getPostsCount()));
                    tvFollowersCount.setText(String.valueOf(user.getFollowersCount()));
                    tvFollowingCount.setText(String.valueOf(user.getFollowingCount()));

                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop()
                                .into(ivProfileAvatar);
                    } else {
                        ivProfileAvatar.setImageResource(R.drawable.bg_circle_avatar);
                    }
    }

    private User loadedTargetUser;

    private void openPrivateChat() {
        if (loadedTargetUser == null || currentUserId == null) {
            Toast.makeText(this, "Đang tải hồ sơ người dùng, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        ChatService.getInstance().getOrCreateConversation(currentUserId, loadedTargetUser,
                new ChatService.Callback<String>() {
                    @Override public void onSuccess(String conversationId) {
                        Intent intent = new Intent(ProfileActivity.this, ChatConversationActivity.class);
                        intent.putExtra(ChatConversationActivity.EXTRA_CONVERSATION_ID, conversationId);
                        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_USER_ID, loadedTargetUser.getId());
                        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_NAME,
                                loadedTargetUser.getFullName() == null ? loadedTargetUser.getUsername() : loadedTargetUser.getFullName());
                        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_AVATAR, loadedTargetUser.getAvatarUrl());
                        startActivity(intent);
                    }
                    @Override public void onError(Exception error) {
                        Toast.makeText(ProfileActivity.this,
                                "Không mở được tin nhắn: " + (error.getMessage() == null ? "Lỗi Firebase" : error.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
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
        btnProfileActionButton.setEnabled(true);
        if (isFollowingTarget) {
            btnProfileActionButton.setText("Đang theo dõi");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_profile_action);
            btnProfileActionButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            btnProfileActionButton.setText("Theo dõi");
            btnProfileActionButton.setBackgroundResource(R.drawable.bg_profile_action);
            btnProfileActionButton.setTextColor(ContextCompat.getColor(this, R.color.dlog_on_primary));
        }
        btnProfileActionButton.setElevation(getResources().getDisplayMetrics().density * 4f);
    }

    private void handleFollowToggle() {
        if (!btnProfileActionButton.isEnabled()) return;
        btnProfileActionButton.setEnabled(false);
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
                    btnProfileActionButton.setEnabled(true);
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
                    btnProfileActionButton.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Lỗi theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadUserPosts() {
        progressBarProfile.setVisibility(View.VISIBLE);
        tvStateProfile.setVisibility(View.GONE);
        final com.example.myapplication.service.impl.OfflinePages offline =
                new com.example.myapplication.service.impl.OfflinePages(this);
        final List<Post> cachedPosts = offline.profilePosts(targetUserId);
        final boolean hasCache = offline.hasProfilePosts(targetUserId);
        if (hasCache) {
            displayPosts(cachedPosts);
            tvPostsCount.setText(String.valueOf(cachedPosts.size()));
            tvOfflineProfile.setText("SQLite · Đang hiển thị ảnh hồ sơ đã lưu và kiểm tra dữ liệu mới…");
            tvOfflineProfile.setVisibility(View.GONE);
        }

        if (!com.example.myapplication.util.NetworkState.isOnline(this)) {
            progressBarProfile.setVisibility(View.GONE);
            tvOfflineProfile.setVisibility(View.GONE);
            tvStateProfile.setVisibility(cachedPosts.isEmpty() ? View.VISIBLE : View.GONE);
            if (cachedPosts.isEmpty()) {
                tvStateProfile.setText("Lưới ảnh này chưa được mở trong phiên khi có mạng");
            }
            return;
        }

        postService.getPostsByUserId(targetUserId, new PostService.Callback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                progressBarProfile.setVisibility(View.GONE);
                tvOfflineProfile.setVisibility(View.GONE);
                tvPostsCount.setText(String.valueOf(posts.size()));
                offline.saveProfilePosts(targetUserId, posts);

                if (posts.isEmpty()) {
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa có bài viết nào");
                } else {
                    tvStateProfile.setVisibility(View.GONE);
                }
                displayPosts(posts);
            }

            @Override
            public void onError(Exception e) {
                progressBarProfile.setVisibility(View.GONE);
                if (hasCache) {
                    tvOfflineProfile.setText("Ngoại tuyến · Ảnh hồ sơ đang đọc từ SQLite");
                    tvOfflineProfile.setVisibility(View.GONE);
                    tvStateProfile.setVisibility(cachedPosts.isEmpty() ? View.VISIBLE : View.GONE);
                    if (cachedPosts.isEmpty()) tvStateProfile.setText("Hồ sơ đã lưu chưa có ảnh");
                } else {
                    tvOfflineProfile.setVisibility(View.GONE);
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa từng mở lưới ảnh này khi có mạng");
                }
            }
        });
    }

    private void loadSavedPosts() {
        progressBarProfile.setVisibility(View.VISIBLE);
        tvStateProfile.setVisibility(View.GONE);
        final com.example.myapplication.service.impl.OfflinePages offline =
                new com.example.myapplication.service.impl.OfflinePages(this);
        final List<Post> cachedPosts = offline.savedPosts(targetUserId);
        final boolean hasCache = offline.hasSavedPosts(targetUserId);
        if (hasCache) {
            displayPosts(cachedPosts);
            tvOfflineProfile.setText("SQLite · Đang hiển thị mục Đã lưu và kiểm tra dữ liệu mới…");
            tvOfflineProfile.setVisibility(View.GONE);
        }

        if (!com.example.myapplication.util.NetworkState.isOnline(this)) {
            progressBarProfile.setVisibility(View.GONE);
            tvOfflineProfile.setVisibility(View.GONE);
            tvStateProfile.setVisibility(cachedPosts.isEmpty() ? View.VISIBLE : View.GONE);
            if (cachedPosts.isEmpty()) {
                tvStateProfile.setText("Mục Đã lưu chưa được mở trong phiên khi có mạng");
            }
            return;
        }

        postService.getSavedPosts(targetUserId, new PostService.Callback<List<Post>>() {
            @Override
            public void onSuccess(List<Post> posts) {
                progressBarProfile.setVisibility(View.GONE);
                tvOfflineProfile.setVisibility(View.GONE);
                offline.saveSavedPosts(targetUserId, posts);
                if (posts.isEmpty()) {
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa có bài viết đã lưu");
                } else {
                    tvStateProfile.setVisibility(View.GONE);
                }
                displayPosts(posts);
            }

            @Override
            public void onError(Exception e) {
                progressBarProfile.setVisibility(View.GONE);
                if (hasCache) {
                    tvOfflineProfile.setText("Ngoại tuyến · Mục Đã lưu đang đọc từ SQLite");
                    tvOfflineProfile.setVisibility(View.GONE);
                    tvStateProfile.setVisibility(cachedPosts.isEmpty() ? View.VISIBLE : View.GONE);
                    if (cachedPosts.isEmpty()) tvStateProfile.setText("Mục Đã lưu ngoại tuyến đang trống");
                } else {
                    tvOfflineProfile.setVisibility(View.GONE);
                    tvStateProfile.setVisibility(View.VISIBLE);
                    tvStateProfile.setText("Chưa từng mở mục Đã lưu khi có mạng");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (targetUserId == null) return;
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.PROFILE);
        loadUserProfile();
        if (isShowingSavedPosts) {
            loadSavedPosts();
        } else {
            loadUserPosts();
        }
    }

    @Override
    protected void onStop() {
        if (profileRegistration != null) {
            profileRegistration.remove();
            profileRegistration = null;
        }
        super.onStop();
    }
}
