package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.adapter.PostAdapter;
import com.example.myapplication.adapter.UserStripAdapter;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;
import com.example.myapplication.util.FeedPageSelector;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvFeed;
    private ProgressBar progressBarFeed;
    private TextView tvStateFeed;
    private TextView tvOfflineFeed;
    private RecyclerView rvPeople;
    private ImageButton btnSearchHome;
    private ImageButton btnNotificationsHome;
    private ImageButton btnMenuHome;
    private ImageButton btnScrollToTop;
    private DrawerLayout homeDrawer;
    private AppBarLayout feedAppBar;

    private PostAdapter postAdapter;
    private PostService postService;
    private ListenerRegistration postRealtimeRegistration;
    private AuthService authService;
    private UserStripAdapter userStripAdapter;
    private ListenerRegistration currentUserRegistration;

    private DocumentSnapshot lastVisibleDocument;
    private boolean isLoadingFeed = false;
    private boolean hasMorePosts = true;
    private static final int PAGE_SIZE = 10;
    private int feedRequestGeneration = 0;
    private final Handler feedHandler = new Handler(Looper.getMainLooper());
    private final Runnable ownPostExpiry = new Runnable() {
        @Override public void run() { loadFeedPosts(true); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (homeDrawer != null && homeDrawer.isDrawerOpen(GravityCompat.START)) {
                    homeDrawer.closeDrawer(GravityCompat.START);
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
        setupPeopleStrip();
        setupRecyclerView();
        loadFeedPosts(true);
        observeVisiblePostChanges();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvFeed = findViewById(R.id.rvFeed);
        progressBarFeed = findViewById(R.id.progressBarFeed);
        tvStateFeed = findViewById(R.id.tvStateFeed);
        tvOfflineFeed = findViewById(R.id.tvOfflineFeed);
        rvPeople = findViewById(R.id.rvPeople);
        btnSearchHome = findViewById(R.id.btnSearchHome);
        btnNotificationsHome = findViewById(R.id.btnNotificationsHome);
        btnMenuHome = findViewById(R.id.btnMenuHome);
        btnScrollToTop = findViewById(R.id.btnScrollToTop);
        homeDrawer = findViewById(R.id.homeDrawer);
        feedAppBar = findViewById(R.id.feedAppBar);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                loadPeople();
                loadFeedPosts(true);
            }
        });

        btnSearchHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SearchActivity.class));
            }
        });

        btnNotificationsHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, NotificationActivity.class));
            }
        });
        btnMenuHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeDrawer.openDrawer(GravityCompat.START);
            }
        });
        findViewById(R.id.quickCreatePost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, CreatePostActivity.class));
            }
        });
        btnScrollToTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rvFeed.smoothScrollToPosition(0);
                feedAppBar.setExpanded(true, true);
                btnScrollToTop.setVisibility(View.GONE);
            }
        });
        setupDrawerMenu();
    }

    private void setupPeopleStrip() {
        rvPeople.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        userStripAdapter = new UserStripAdapter(this, authService.getCurrentUserId());
        rvPeople.setAdapter(userStripAdapter);
    }

    private void setupDrawerMenu() {
        findViewById(R.id.rowMenuProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile(false);
            }
        });
        findViewById(R.id.rowMenuSaved).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile(true);
            }
        });
        findViewById(R.id.rowMenuCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, CreatePostActivity.class));
            }
        });
        findViewById(R.id.rowMenuNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, NotificationActivity.class));
            }
        });
        findViewById(R.id.rowMenuChats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ChatsActivity.class));
            }
        });
        findViewById(R.id.rowMenuSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            }
        });
        findViewById(R.id.rowMenuAdmin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AdminActivity.class));
            }
        });
        findViewById(R.id.rowMenuHelp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Trung tâm trợ giúp sẽ sớm có mặt.", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.rowMenuLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authService.logout();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void openProfile(boolean showSaved) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("showSavedPosts", showSaved);
        startActivity(intent);
    }

    private void loadDrawerUser() {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;
        ImageView avatar = findViewById(R.id.ivMenuAvatar);
        TextView name = findViewById(R.id.tvMenuName);
        TextView username = findViewById(R.id.tvMenuUsername);
        if (currentUserRegistration != null) return;
        currentUserRegistration = UserService.getInstance().observeUserProfile(userId, new UserService.Callback<User>() {
            @Override public void onSuccess(User user) {
                if (user == null) return;
                findViewById(R.id.rowMenuAdmin).setVisibility(
                        "admin".equalsIgnoreCase(user.getRole()) ? View.VISIBLE : View.GONE);
                name.setText(user.getFullName() == null || user.getFullName().isEmpty()
                        ? "Người dùng DLog" : user.getFullName());
                username.setText("@" + (user.getUsername() == null ? "dlog_user" : user.getUsername()));
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(HomeActivity.this).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
                    Glide.with(HomeActivity.this).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop()
                            .into((ImageView) findViewById(R.id.ivQuickCreateAvatar));
                } else {
                    avatar.setImageResource(R.drawable.bg_circle_avatar);
                    ((ImageView) findViewById(R.id.ivQuickCreateAvatar)).setImageResource(R.drawable.bg_circle_avatar);
                }
            }
            @Override public void onError(Exception e) { }
        });
    }

    private void loadPeople() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            userStripAdapter.setUsers(null);
            showPeopleSection(false);
            return;
        }
        UserService.getInstance().getFollowUsers(userId, true, new UserService.Callback<List<User>>() {
            @Override public void onSuccess(List<User> users) {
                if (isFinishing() || isDestroyed()) return;
                userStripAdapter.setUsers(users);
                showPeopleSection(userStripAdapter.getItemCount() > 0);
            }

            @Override public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) return;
                showPeopleSection(userStripAdapter.getItemCount() > 0);
            }
        });
    }

    private void showPeopleSection(boolean visible) {
        findViewById(R.id.peopleSection).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvFeed.setLayoutManager(layoutManager);
        rvFeed.setHasFixedSize(false);

        postAdapter = new PostAdapter(this);
        postAdapter.setOnPostChangeListener(new PostAdapter.OnPostChangeListener() {
            @Override
            public void onPostUpdated() {
                // The adapter already updates the edited item locally.
            }

            @Override
            public void onPostDeleted(String postId) {
                if (postAdapter.getItemCount() == 0) {
                    tvStateFeed.setVisibility(View.VISIBLE);
                    tvStateFeed.setText("Chưa có bài viết từ người khác. Bạn có thể xem bài của mình trong trang cá nhân.");
                }
            }
        });
        rvFeed.setAdapter(postAdapter);

        // Load the next page when the user approaches the end of the feed.
        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                btnScrollToTop.setVisibility(firstVisibleItem >= 3 ? View.VISIBLE : View.GONE);

                if (dy <= 0 || isLoadingFeed || !hasMorePosts) {
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();

                if (totalItemCount > 0
                        && visibleItemCount + firstVisibleItem >= totalItemCount - 2) {
                    loadFeedPosts(false);
                }
            }
        });
    }

    private void loadFeedPosts(boolean isRefresh) {
        if (isRefresh) {
            List<Post> saved = new com.example.myapplication.service.impl.OfflinePages(this)
                    .feed(authService.getCurrentUserId());
            if (!saved.isEmpty()) {
                postAdapter.setPosts(saved);
                tvStateFeed.setVisibility(View.GONE);
                tvOfflineFeed.setText("SQLite · Đang hiển thị bảng tin đã lưu và kiểm tra dữ liệu mới…");
                tvOfflineFeed.setVisibility(View.GONE);
            }
        }
        if (isRefresh) {
            feedRequestGeneration++;
            lastVisibleDocument = null;
            hasMorePosts = true;
            isLoadingFeed = true;

            if (!swipeRefreshLayout.isRefreshing()) {
                progressBarFeed.setVisibility(View.VISIBLE);
            }
            tvStateFeed.setVisibility(View.GONE);
        } else {
            if (isLoadingFeed || !hasMorePosts) {
                return;
            }
            isLoadingFeed = true;
        }

        final int requestGeneration = feedRequestGeneration;
        postService.getFeedPosts(authService.getCurrentUserId(), lastVisibleDocument, PAGE_SIZE,
                new PostService.Callback<PostService.FeedResult>() {
                    @Override
                    public void onSuccess(PostService.FeedResult result) {
                        if (requestGeneration != feedRequestGeneration || isDestroyed()) return;
                        swipeRefreshLayout.setRefreshing(false);
                        progressBarFeed.setVisibility(View.GONE);
                        isLoadingFeed = false;
                        tvOfflineFeed.setVisibility(View.GONE);

                        if (result == null) {
                            if (isRefresh && postAdapter.getItemCount() == 0) {
                                tvStateFeed.setVisibility(View.VISIBLE);
                                tvStateFeed.setText("Chưa có dữ liệu để hiển thị");
                            }
                            return;
                        }

                        List<Post> posts = result.getPosts();
                        lastVisibleDocument = result.getLastDocument();
                        hasMorePosts = result.hasMore();

                        if (isRefresh) {
                            postAdapter.setPosts(posts);
                            scheduleOwnPostExpiry(posts);
                            if (posts != null && !posts.isEmpty()) {
                                new com.example.myapplication.service.impl.OfflinePages(HomeActivity.this)
                                        .saveFeedPage(authService.getCurrentUserId(), posts, true);
                            }
                            if (posts == null || posts.isEmpty()) {
                                tvStateFeed.setVisibility(View.VISIBLE);
                                tvStateFeed.setText("Chưa có bài viết nào để hiển thị.");
                            } else {
                                tvStateFeed.setVisibility(View.GONE);
                            }
                        } else {
                            postAdapter.addPosts(posts);
                            if (posts != null && !posts.isEmpty()) {
                                new com.example.myapplication.service.impl.OfflinePages(HomeActivity.this)
                                        .saveFeedPage(authService.getCurrentUserId(), posts, false);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (requestGeneration != feedRequestGeneration || isDestroyed()) return;
                        swipeRefreshLayout.setRefreshing(false);
                        progressBarFeed.setVisibility(View.GONE);
                        isLoadingFeed = false;

                        if (isRefresh && postAdapter.getItemCount() == 0) {
                            tvStateFeed.setVisibility(View.VISIBLE);
                            tvStateFeed.setText("Không thể tải bảng tin. Vui lòng kiểm tra kết nối.");
                        }
                        if (postAdapter.getItemCount() > 0) {
                            tvOfflineFeed.setText(isRefresh
                                    ? "Ngoại tuyến · Bảng tin đang đọc từ SQLite"
                                    : "Ngoại tuyến · Đã hết các trang lưu trong SQLite");
                            tvOfflineFeed.setVisibility(View.GONE);
                            return;
                        }

                        Toast.makeText(HomeActivity.this,
                                "Lỗi tải bảng tin: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void observeVisiblePostChanges() {
        if (postRealtimeRegistration != null) {
            postRealtimeRegistration.remove();
        }
        postRealtimeRegistration = FirebaseFirestore.getInstance()
                .collection("posts")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onEvent(com.google.firebase.firestore.QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException error) {
                        if (error != null || snapshot == null || isFinishing() || isDestroyed()) return;
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            if (document.getData() == null) continue;
                            Post updated = Post.fromMap(document.getData());
                            if (updated != null) {
                                updated.setId(document.getId());
                                postAdapter.updatePostRealtime(updated);
                            }
                        }
                    }
                });
    }

    private void scheduleOwnPostExpiry(List<Post> posts) {
        feedHandler.removeCallbacks(ownPostExpiry);
        if (posts == null) return;
        String viewerId = authService.getCurrentUserId();
        long soonest = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (Post post : posts) {
            if (post != null && viewerId != null && viewerId.equals(post.getAuthorId())) {
                long expiry = post.getCreatedAt() + FeedPageSelector.OWN_POST_VISIBILITY_MS;
                if (expiry > now && expiry < soonest) soonest = expiry;
            }
        }
        if (soonest != Long.MAX_VALUE) feedHandler.postDelayed(ownPostExpiry, soonest - now + 100L);
    }

    @Override
    protected void onDestroy() {
        feedHandler.removeCallbacks(ownPostExpiry);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (postRealtimeRegistration != null) {
            postRealtimeRegistration.remove();
            postRealtimeRegistration = null;
        }
        if (currentUserRegistration != null) {
            currentUserRegistration.remove();
            currentUserRegistration = null;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrawerUser();
        loadPeople();
        if (getSharedPreferences("dlog_ui", MODE_PRIVATE)
                .getBoolean("refresh_feed_after_edit", false)) {
            getSharedPreferences("dlog_ui", MODE_PRIVATE).edit()
                    .remove("refresh_feed_after_edit").apply();
            loadFeedPosts(true);
        }
        // Keep this call here so the bottom navigation is re-selected after returning from another screen.
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.FEED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("refresh_after_post", false)) {
            intent.removeExtra("refresh_after_post");
            loadFeedPosts(true);
            rvFeed.scrollToPosition(0);
            feedAppBar.setExpanded(true, false);
            btnScrollToTop.setVisibility(View.GONE);
        }
    }

}
