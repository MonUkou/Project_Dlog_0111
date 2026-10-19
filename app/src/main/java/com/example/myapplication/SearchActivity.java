package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class SearchActivity extends AppCompatActivity {

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
        setupRecyclerView();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.SEARCH);
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        tvEmptySearch = findViewById(R.id.tvEmptySearch);

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
            tvEmptySearch.setVisibility(View.VISIBLE);
            tvEmptySearch.setText("Nhập từ khóa để tìm kiếm");
            searchAdapter.setUsers(new ArrayList<>());
            return;
        }

        progressBarSearch.setVisibility(View.VISIBLE);
        tvEmptySearch.setVisibility(View.GONE);

        userService.searchUsers(query, new UserService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                progressBarSearch.setVisibility(View.GONE);
                if (users.isEmpty()) {
                    tvEmptySearch.setVisibility(View.VISIBLE);
                    tvEmptySearch.setText("Không tìm thấy người dùng nào");
                } else {
                    tvEmptySearch.setVisibility(View.GONE);
                }
                searchAdapter.setUsers(users);
            }

            @Override
            public void onError(Exception e) {
                progressBarSearch.setVisibility(View.GONE);
                tvEmptySearch.setVisibility(View.VISIBLE);
                tvEmptySearch.setText("Lỗi tìm kiếm: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.SEARCH);
    }

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
            holder.tvNotifyText.setText((user.getUsername() != null ? user.getUsername() : user.getId())
                    + (user.getFullName() != null && !user.getFullName().isEmpty() ? " (" + user.getFullName() + ")" : ""));
            holder.vUnreadDot.setVisibility(View.GONE);

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(context).load(user.getAvatarUrl()).circleCrop().into(holder.ivNotifyAvatar);
            } else {
                holder.ivNotifyAvatar.setImageResource(R.drawable.bg_circle_avatar);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", user.getId());
                context.startActivity(intent);
            });
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
        }
    }
}
