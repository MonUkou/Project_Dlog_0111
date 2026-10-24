package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for searching users by username or keyword.
 */
public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBarSearch;
    private TextView tvEmptySearch;

    private UserSearchAdapter searchAdapter;
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        userService = UserService.getInstance();

        initViews();
        setupSearchListener();
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.SEARCH);
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvEmptySearch = findViewById(R.id.tvEmptySearch);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new UserSearchAdapter(this);
        rvSearchResults.setAdapter(searchAdapter);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            showEmptySearchState("Nhập từ khóa để tìm kiếm");
            searchAdapter.setUsers(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Performing user search with query: " + query);
        progressBarSearch.setVisibility(View.VISIBLE);
        tvEmptySearch.setVisibility(View.GONE);

        userService.searchUsers(query, new UserService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                progressBarSearch.setVisibility(View.GONE);
                if (users.isEmpty()) {
                    showEmptySearchState("Không tìm thấy người dùng nào");
                } else {
                    tvEmptySearch.setVisibility(View.GONE);
                }
                searchAdapter.setUsers(users);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error executing user search", e);
                progressBarSearch.setVisibility(View.GONE);
                showEmptySearchState("Lỗi tìm kiếm: " + e.getMessage());
            }
        });
    }

    private void showEmptySearchState(String message) {
        tvEmptySearch.setVisibility(View.VISIBLE);
        tvEmptySearch.setText(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.SEARCH);
    }

    /**
     * RecyclerView Adapter for displaying user search results.
     */
    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {
        private final Context context;
        private final List<User> users = new ArrayList<>();

        public UserSearchAdapter(Context context) {
            this.context = context;
        }

        public void setUsers(List<User> newUsers) {
            this.users.clear();
            if (newUsers != null) {
                this.users.addAll(newUsers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.bind(context, user);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView ivNotifyAvatar;
            TextView tvNotifyText;
            View vUnreadDot;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                ivNotifyAvatar = itemView.findViewById(R.id.ivNotifyAvatar);
                tvNotifyText = itemView.findViewById(R.id.tvNotifyText);
                vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
            }

            public void bind(Context context, User user) {
                String displayName = (user.getUsername() != null ? user.getUsername() : user.getId())
                        + (user.getFullName() != null && !user.getFullName().isEmpty() ? " (" + user.getFullName() + ")" : "");
                tvNotifyText.setText(displayName);
                vUnreadDot.setVisibility(View.GONE);

                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(context).load(user.getAvatarUrl()).circleCrop().into(ivNotifyAvatar);
                } else {
                    ivNotifyAvatar.setImageResource(R.drawable.bg_circle_avatar);
                }

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userId", user.getId());
                    context.startActivity(intent);
                });
            }
        }
    }
}
