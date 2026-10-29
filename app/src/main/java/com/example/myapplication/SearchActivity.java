package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.User;
import com.example.myapplication.model.Post;
import com.example.myapplication.adapter.PostGridAdapter;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.OfflinePages;
import com.example.myapplication.util.BottomNavHelper;
import com.example.myapplication.util.HashtagParser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

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
    private PostGridAdapter tagPostAdapter;
    private RecyclerView rvTagPosts;
    private LinearLayout tagSuggestions;
    private View tagSuggestionsScroll;
    private TextView tabUsers, tabTags;
    private TextView sortNewest, sortOldest, sortPopular;
    private View photoSortBar;
    private boolean tagMode;
    private int photoSortMode;
    private int searchGeneration;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        userService = UserService.getInstance();

        initViews();
        setupSearchListener();
        setupRecyclerView();
        tabUsers = findViewById(R.id.tabSearchUsers);
        tabTags = findViewById(R.id.tabSearchTags);
        sortNewest = findViewById(R.id.sortNewest);
        sortOldest = findViewById(R.id.sortOldest);
        sortPopular = findViewById(R.id.sortPopular);
        photoSortBar = findViewById(R.id.photoSortBar);
        rvTagPosts = findViewById(R.id.rvTagPosts);
        tagSuggestions = findViewById(R.id.tagSuggestions);
        tagSuggestionsScroll = findViewById(R.id.tagSuggestionsScroll);
        rvTagPosts.setLayoutManager(new GridLayoutManager(this, 3));
        tagPostAdapter = new PostGridAdapter(this);
        rvTagPosts.setAdapter(tagPostAdapter);
        tabUsers.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { setTagMode(false); }
        });
        tabTags.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { setTagMode(true); }
        });
        sortNewest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { setPhotoSortMode(0); }
        });
        sortOldest.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { setPhotoSortMode(1); }
        });
        sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { setPhotoSortMode(2); }
        });
        updateSortControls();
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
                if (s.length() > 0 && s.charAt(0) == '#' && !tagMode) setTagMode(true);
                scheduleSearch();
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
        final int generation = ++searchGeneration;
        if (tagMode) {
            searchTags(query, generation);
            return;
        }
        if (query.isEmpty()) {
            progressBarSearch.setVisibility(View.GONE);
            showEmptySearchState("Nhập tên để tìm tài khoản");
            searchAdapter.setUsers(new ArrayList<>());
            return;
        }

        Log.d(TAG, "Performing user search with query: " + query);
        progressBarSearch.setVisibility(View.VISIBLE);
        tvEmptySearch.setVisibility(View.GONE);

        userService.searchUsers(query, new UserService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (generation != searchGeneration || isFinishing()) return;
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
                if (generation != searchGeneration || isFinishing()) return;
                Log.e(TAG, "Error executing user search", e);
                progressBarSearch.setVisibility(View.GONE);
                showEmptySearchState("Lỗi tìm kiếm: " + e.getMessage());
            }
        });
    }

    private void setTagMode(boolean enabled) {
        tagMode = enabled;
        rvSearchResults.setVisibility(enabled ? View.GONE : View.VISIBLE);
        rvTagPosts.setVisibility(enabled ? View.VISIBLE : View.GONE);
        tagSuggestionsScroll.setVisibility(enabled ? View.VISIBLE : View.GONE);
        photoSortBar.setVisibility(View.GONE);
        tabUsers.setTextColor(getColor(enabled ? R.color.dlog_muted_blue : R.color.dlog_blue));
        tabTags.setTextColor(getColor(enabled ? R.color.dlog_blue : R.color.dlog_muted_blue));
        tabUsers.setTypeface(null, enabled ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        tabTags.setTypeface(null, enabled ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        etSearch.setHint(enabled ? "Tìm #hashtag của ảnh" : "Tìm tài khoản");
        if (enabled && tagSuggestions.getChildCount() == 0) loadTagSuggestions();
        scheduleSearch();
    }

    private void setPhotoSortMode(int mode) {
        if (photoSortMode == mode) return;
        photoSortMode = mode;
        updateSortControls();
        if (tagMode) scheduleSearch();
    }

    private void updateSortControls() {
        TextView[] controls = {sortNewest, sortOldest, sortPopular};
        for (int i = 0; i < controls.length; i++) {
            boolean selected = i == photoSortMode;
            controls[i].setTextColor(getColor(selected ? R.color.dlog_blue : R.color.dlog_muted_blue));
            controls[i].setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            controls[i].setSelected(selected);
        }
    }

    private void scheduleSearch() {
        searchGeneration++;
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        final String query = etSearch.getText().toString().trim();
        pendingSearch = new Runnable() {
            @Override public void run() { performSearch(query); }
        };
        searchHandler.postDelayed(pendingSearch, 250);
    }

    private void searchTags(String query, final int generation) {
        if (query.isEmpty()) {
            progressBarSearch.setVisibility(View.GONE);
            rvTagPosts.setVisibility(View.GONE);
            tagPostAdapter.setPosts(new ArrayList<Post>());
            showEmptySearchState("Chọn chủ đề bên trên hoặc nhập #hashtag");
            return;
        }
        rvTagPosts.setVisibility(View.VISIBLE);
        String value = query.startsWith("#") ? query : "#" + query;
        List<String> tags = HashtagParser.extract(value);
        progressBarSearch.setVisibility(View.VISIBLE);
        tvEmptySearch.setVisibility(View.GONE);
        Query photoQuery;
        if (query.isEmpty()) {
            String field = photoSortMode == 2 ? "likesCount" : "createdAt";
            Query.Direction direction = photoSortMode == 1
                    ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
            photoQuery = FirebaseFirestore.getInstance().collection("posts")
                    .orderBy(field, direction).limit(100);
        } else if (!tags.isEmpty()) {
            photoQuery = FirebaseFirestore.getInstance().collection("posts")
                    .whereArrayContains("hashtags", tags.get(0));
        } else {
            progressBarSearch.setVisibility(View.GONE);
            tagPostAdapter.setPosts(new ArrayList<Post>());
            showEmptySearchState("Nhập hashtag hợp lệ để tìm ảnh");
            return;
        }
        photoQuery.get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override public void onSuccess(com.google.firebase.firestore.QuerySnapshot snapshot) {
                    if (generation != searchGeneration || isFinishing()) return;
                    List<Post> posts = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Post post = Post.fromMap(document.getData());
                        if (post != null && !post.isHidden()) {
                            post.setId(document.getId());
                            posts.add(post);
                        }
                    }
                    sortPhotoResults(posts);
                    progressBarSearch.setVisibility(View.GONE);
                    tagPostAdapter.setPosts(posts);
                    if (posts.isEmpty()) showEmptySearchState(query.isEmpty()
                            ? "Chưa có ảnh để khám phá" : "Chưa có ảnh với hashtag này");
                    else tvEmptySearch.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) {
                    if (generation != searchGeneration || isFinishing()) return;
                    progressBarSearch.setVisibility(View.GONE);
                    OfflinePages offline = new OfflinePages(SearchActivity.this);
                    List<Post> cached = query.isEmpty()
                            ? offline.feed(AuthService.getInstance().getCurrentUserId())
                            : offline.postsWithTag(AuthService.getInstance().getCurrentUserId(), query);
                    sortPhotoResults(cached);
                    tagPostAdapter.setPosts(cached);
                    if (cached.isEmpty()) showEmptySearchState("Chưa có ảnh đã xem để hiển thị ngoại tuyến.");
                    else showEmptySearchState("Ngoại tuyến · Kết quả từ những trang đã xem");
                    }
                });
    }

    private void sortPhotoResults(List<Post> posts) {
        Collections.sort(posts, new Comparator<Post>() {
            @Override public int compare(Post left, Post right) {
                int result;
                if (photoSortMode == 2) {
                    result = Long.compare(right.getLikesCount(), left.getLikesCount());
                    if (result != 0) return result;
                }
                result = photoSortMode == 1
                        ? Long.compare(left.getCreatedAt(), right.getCreatedAt())
                        : Long.compare(right.getCreatedAt(), left.getCreatedAt());
                if (result != 0) return result;
                return String.valueOf(left.getId()).compareTo(String.valueOf(right.getId()));
            }
        });
    }

    private void loadTagSuggestions() {
        FirebaseFirestore.getInstance().collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(60).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override public void onSuccess(com.google.firebase.firestore.QuerySnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    final Map<String, Integer> counts = new HashMap<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Post post = Post.fromMap(document.getData());
                        if (post == null || post.isHidden()) continue;
                        for (String tag : post.getHashtags()) {
                            if (tag != null && tag.length() > 1)
                                counts.put(tag, counts.containsKey(tag) ? counts.get(tag) + 1 : 1);
                        }
                    }
                    List<String> popular = new ArrayList<>(counts.keySet());
                    Collections.sort(popular, new Comparator<String>() {
                        @Override public int compare(String left, String right) {
                            int count = Integer.compare(counts.get(right), counts.get(left));
                            return count != 0 ? count : left.compareTo(right);
                        }
                    });
                    tagSuggestions.removeAllViews();
                    for (int i = 0; i < Math.min(8, popular.size()); i++) {
                        final String tag = popular.get(i);
                        TextView chip = new TextView(SearchActivity.this);
                        chip.setText(tag);
                        chip.setTextColor(getColor(R.color.dlog_blue));
                        chip.setTextSize(14);
                        chip.setGravity(android.view.Gravity.CENTER);
                        chip.setMinHeight((int) (44 * getResources().getDisplayMetrics().density));
                        int inset = (int) (16 * getResources().getDisplayMetrics().density);
                        chip.setPadding(inset, 0, inset, 0);
                        chip.setBackgroundResource(R.drawable.bg_rounded_edittext);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
                        tagSuggestions.addView(chip, params);
                        chip.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View view) { etSearch.setText(tag); etSearch.setSelection(tag.length()); }
                        });
                    }
                    tagSuggestionsScroll.setVisibility(tagMode && !popular.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override protected void onDestroy() {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        super.onDestroy();
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

            public void bind(final Context context, final User user) {
                String displayName = (user.getUsername() != null ? user.getUsername() : user.getId())
                        + (user.getFullName() != null && !user.getFullName().isEmpty() ? " (" + user.getFullName() + ")" : "");
                tvNotifyText.setText(displayName);
                vUnreadDot.setVisibility(View.GONE);

                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(ivNotifyAvatar);
                } else {
                    ivNotifyAvatar.setImageResource(R.drawable.bg_circle_avatar);
                }

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        Intent intent = new Intent(context, ProfileActivity.class);
                        intent.putExtra("userId", user.getId());
                        context.startActivity(intent);
                    }
                });
            }
        }
    }
}
