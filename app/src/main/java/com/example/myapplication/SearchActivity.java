package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.UserService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private TextView tvEmpty;
    private ProgressBar progressBarSearch;

    private UserService userService;
    private AuthService authService;
    private UserAdapter userAdapter;
    private String currentUserId;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch = findViewById(R.id.etSearch);
        RecyclerView rvSearchResults = findViewById(R.id.rvSearchResults);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBarSearch = findViewById(R.id.progressBarSearch);

        userService = new UserService();
        authService = new AuthService();
        currentUserId = authService.getCurrentUserId();

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(user -> {
            Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", user.getId());
            startActivity(intent);
        }, (user, btnFollow) -> {
            if (currentUserId == null || user.getId().equals(currentUserId)) {
                return;
            }
            executorService.execute(() -> {
                try {
                    boolean isFollowing = userService.isFollowing(currentUserId, user.getId());
                    if (isFollowing) {
                        userService.unfollowUser(currentUserId, user.getId());
                        runOnUiThread(() -> {
                            btnFollow.setText("Theo dõi");
                            btnFollow.setBackgroundColor(getResources().getColor(android.R.color.black));
                        });
                    } else {
                        userService.followUser(currentUserId, user.getId());
                        runOnUiThread(() -> {
                            btnFollow.setText("Đang theo dõi");
                            btnFollow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(SearchActivity.this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        });
        rvSearchResults.setAdapter(userAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    searchUsers(query);
                } else {
                    userAdapter.setUsers(null);
                    tvEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup bottom navigation bar
        findViewById(R.id.navFeed).setOnClickListener(v -> finish());
        findViewById(R.id.navSearch).setOnClickListener(v -> {});
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    private void searchUsers(String query) {
        progressBarSearch.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                List<User> results = userService.searchUsers(query);
                runOnUiThread(() -> {
                    progressBarSearch.setVisibility(View.GONE);
                    userAdapter.setUsers(results);
                    if (results.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBarSearch.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
