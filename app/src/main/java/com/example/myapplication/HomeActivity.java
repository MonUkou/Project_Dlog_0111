package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.adapter.PostAdapter;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvFeed;
    private ProgressBar progressBarFeed;
    private TextView tvStateFeed;

    private PostAdapter postAdapter;
    private PostService postService;
    private AuthService authService;

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
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.FEED);

        loadFeedPosts(true);
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvFeed = findViewById(R.id.rvFeed);
        progressBarFeed = findViewById(R.id.progressBarFeed);
        tvStateFeed = findViewById(R.id.tvStateFeed);

        swipeRefreshLayout.setOnRefreshListener(() -> loadFeedPosts(true));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvFeed.setLayoutManager(layoutManager);

        postAdapter = new PostAdapter(this);
        postAdapter.setOnPostChangeListener(new PostAdapter.OnPostChangeListener() {
            @Override
            public void onPostUpdated() {}

            @Override
            public void onPostDeleted(String postId) {
                if (postAdapter.getItemCount() == 0) {
                    tvStateFeed.setVisibility(View.VISIBLE);
                    tvStateFeed.setText("Chưa có bài viết nào");
                }
            }
        });
        rvFeed.setAdapter(postAdapter);

        // Pagination load more scroll listener
        rvFeed.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoadingMore && hasMorePosts) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 2) {
                        loadFeedPosts(false);
                    }
                }
            }
        });
    }

    private void loadFeedPosts(boolean isRefresh) {
        if (isRefresh) {
            lastVisibleDocument = null;
            hasMorePosts = true;
            if (!swipeRefreshLayout.isRefreshing()) {
                progressBarFeed.setVisibility(View.VISIBLE);
            }
            tvStateFeed.setVisibility(View.GONE);
        } else {
            isLoadingMore = true;
        }

        postService.getFeedPosts(lastVisibleDocument, PAGE_SIZE, new PostService.Callback<PostService.FeedResult>() {
            @Override
            public void onSuccess(PostService.FeedResult result) {
                swipeRefreshLayout.setRefreshing(false);
                progressBarFeed.setVisibility(View.GONE);
                isLoadingMore = false;

                List<Post> posts = result.getPosts();
                lastVisibleDocument = result.getLastDocument();
                hasMorePosts = result.hasMore();

                if (isRefresh) {
                    if (posts.isEmpty()) {
                        tvStateFeed.setVisibility(View.VISIBLE);
                        tvStateFeed.setText("Chưa có bài viết nào");
                        postAdapter.setPosts(posts);
                    } else {
                        tvStateFeed.setVisibility(View.GONE);
                        postAdapter.setPosts(posts);
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
                    tvStateFeed.setText("Không thể tải bảng tin (Lỗi kết nối hoặc mất mạng)");
                }
                Toast.makeText(HomeActivity.this, "Lỗi tải bảng tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.FEED);
    }
}
