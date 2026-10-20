package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.adapter.PostAdapter;
import com.example.myapplication.adapter.UserStripAdapter;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvFeed;
    private ProgressBar progressBarFeed;
    private TextView tvStateFeed;
    private RecyclerView rvPeople;
    private ImageButton btnSearchHome;
    private ImageButton btnNotificationsHome;

    private PostAdapter postAdapter;
    private PostService postService;
    private AuthService authService;
    private UserStripAdapter userStripAdapter;

    private DocumentSnapshot lastVisibleDocument;
    private boolean isLoadingMore = false;
    private boolean hasMorePosts = true;
    private static final int PAGE_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        postService = PostService.getInstance();
        authService = AuthService.getInstance();
        authService.init(this);

        initViews();
        setupPeopleStrip();
        setupRecyclerView();
        loadPeople();
        loadFeedPosts(true);
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvFeed = findViewById(R.id.rvFeed);
        progressBarFeed = findViewById(R.id.progressBarFeed);
        tvStateFeed = findViewById(R.id.tvStateFeed);
        rvPeople = findViewById(R.id.rvPeople);
        btnSearchHome = findViewById(R.id.btnSearchHome);
        btnNotificationsHome = findViewById(R.id.btnNotificationsHome);

        swipeRefreshLayout.setOnRefreshListener(() -> loadFeedPosts(true));

        btnSearchHome.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SearchActivity.class)));

        btnNotificationsHome.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NotificationActivity.class)));
    }

    private void setupPeopleStrip() {
        rvPeople.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        userStripAdapter = new UserStripAdapter(this, authService.getCurrentUserId());
        rvPeople.setAdapter(userStripAdapter);
    }

    private void loadPeople() {
        FirebaseFirestore.getInstance().collection("users").limit(30).get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new java.util.ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        if (document.getData() != null) {
                            User user = User.fromMap(document.getData());
                            if (user != null) {
                                if (user.getId() == null) user.setId(document.getId());
                                users.add(user);
                            }
                        }
                    }
                    userStripAdapter.setUsers(users);
                });
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
                    tvStateFeed.setText("Chưa có bài viết nào");
                }
            }
        });
        rvFeed.setAdapter(postAdapter);

        // Load the next page when the user approaches the end of the feed.
        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || isLoadingMore || !hasMorePosts) {
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                if (totalItemCount > 0
                        && visibleItemCount + firstVisibleItem >= totalItemCount - 2) {
                    loadFeedPosts(false);
                }
            }
        });
    }

    private void loadFeedPosts(boolean isRefresh) {
        if (isRefresh) {
            lastVisibleDocument = null;
            hasMorePosts = true;
            isLoadingMore = false;

            if (!swipeRefreshLayout.isRefreshing()) {
                progressBarFeed.setVisibility(View.VISIBLE);
            }
            tvStateFeed.setVisibility(View.GONE);
        } else {
            if (isLoadingMore || !hasMorePosts) {
                return;
            }
            isLoadingMore = true;
        }

        postService.getFeedPosts(lastVisibleDocument, PAGE_SIZE,
                new PostService.Callback<PostService.FeedResult>() {
                    @Override
                    public void onSuccess(PostService.FeedResult result) {
                        swipeRefreshLayout.setRefreshing(false);
                        progressBarFeed.setVisibility(View.GONE);
                        isLoadingMore = false;

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
                            if (posts == null || posts.isEmpty()) {
                                tvStateFeed.setVisibility(View.VISIBLE);
                                tvStateFeed.setText("Chưa có bài viết nào");
                            } else {
                                tvStateFeed.setVisibility(View.GONE);
                            }
                        } else {
                            postAdapter.addPosts(posts);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        swipeRefreshLayout.setRefreshing(false);
                        progressBarFeed.setVisibility(View.GONE);
                        isLoadingMore = false;

                        if (isRefresh && postAdapter.getItemCount() == 0) {
                            tvStateFeed.setVisibility(View.VISIBLE);
                            tvStateFeed.setText("Không thể tải bảng tin. Vui lòng kiểm tra kết nối.");
                        }

                        Toast.makeText(HomeActivity.this,
                                "Lỗi tải bảng tin: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep this call here so the bottom navigation is re-selected after returning from another screen.
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.FEED);
    }
}
