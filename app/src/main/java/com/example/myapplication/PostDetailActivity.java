package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.CommentAdapter;
import com.example.myapplication.model.Comment;
import com.example.myapplication.model.Like;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.InteractionService;
import com.example.myapplication.service.impl.NotificationService;
import com.example.myapplication.service.impl.PostService;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private ImageView btnBackDetail;
    private TextView btnEditPost;

    // Post Card elements inside postCardDetail include
    private View postCardDetail;
    private View llAuthorHeader;
    private ImageView ivAuthorAvatar;
    private TextView tvAuthorUsername;
    private ImageView ivPostImage;
    private TextView tvImagePlaceholder;
    private TextView btnLike;
    private TextView btnComment;
    private TextView btnSave;
    private TextView tvLikesCount;
    private TextView tvCaption;

    // Comments section
    private RecyclerView rvComments;
    private ProgressBar progressBarComments;
    private TextView tvNoComments;
    private EditText etCommentInput;
    private Button btnSendComment;

    private CommentAdapter commentAdapter;
    private PostService postService;
    private InteractionService interactionService;
    private AuthService authService;
    private NotificationService notificationService;

    private String postId;
    private Post currentPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra("postId");
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "Bài viết không hợp lệ!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        postService = PostService.getInstance();
        interactionService = InteractionService.getInstance();
        authService = AuthService.getInstance();
        notificationService = NotificationService.getInstance();

        initViews();
        setupCommentsRecyclerView();

        loadPostDetail();
        loadComments();
    }

    private void initViews() {
        btnBackDetail = findViewById(R.id.btnBackDetail);
        btnEditPost = findViewById(R.id.btnEditPost);

        postCardDetail = findViewById(R.id.postCardDetail);
        llAuthorHeader = postCardDetail.findViewById(R.id.llAuthorHeader);
        ivAuthorAvatar = postCardDetail.findViewById(R.id.ivAuthorAvatar);
        tvAuthorUsername = postCardDetail.findViewById(R.id.tvAuthorUsername);
        ivPostImage = postCardDetail.findViewById(R.id.ivPostImage);
        tvImagePlaceholder = postCardDetail.findViewById(R.id.tvImagePlaceholder);
        btnLike = postCardDetail.findViewById(R.id.btnLike);
        btnComment = postCardDetail.findViewById(R.id.btnComment);
        btnSave = postCardDetail.findViewById(R.id.btnSave);
        tvLikesCount = postCardDetail.findViewById(R.id.tvLikesCount);
        tvCaption = postCardDetail.findViewById(R.id.tvCaption);

        rvComments = findViewById(R.id.rvComments);
        progressBarComments = findViewById(R.id.progressBarComments);
        tvNoComments = findViewById(R.id.tvNoComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        btnBackDetail.setOnClickListener(v -> finish());

        btnSendComment.setOnClickListener(v -> handleSendComment());
    }

    private void setupCommentsRecyclerView() {
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this);
        rvComments.setAdapter(commentAdapter);
    }

    private void loadPostDetail() {
        String currentUserId = authService.getCurrentUserId();

        postService.getPostDetail(postId, new PostService.Callback<Post>() {
            @Override
            public void onSuccess(Post post) {
                currentPost = post;
                renderPostCard(post, currentUserId);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PostDetailActivity.this, "Không thể tải bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderPostCard(Post post, String currentUserId) {
        String username = post.getAuthorUsername() != null ? post.getAuthorUsername() : post.getAuthorId();
        tvAuthorUsername.setText(username);

        if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(post.getAuthorAvatarUrl())
                    .circleCrop()
                    .into(ivAuthorAvatar);
        } else {
            ivAuthorAvatar.setImageResource(R.drawable.bg_circle_avatar);
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            tvImagePlaceholder.setVisibility(View.GONE);
            Glide.with(this)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .into(ivPostImage);
        } else {
            ivPostImage.setImageDrawable(null);
            tvImagePlaceholder.setVisibility(View.VISIBLE);
        }

        tvLikesCount.setText(post.getLikesCount() + " lượt thích");
        String captionText = username + " " + (post.getCaption() != null ? post.getCaption() : "");
        tvCaption.setText(captionText);

        // Show Edit/Delete option on top right header if current user is author
        boolean isAuthor = currentUserId != null && currentUserId.equals(post.getAuthorId());
        if (isAuthor) {
            btnEditPost.setVisibility(View.VISIBLE);
            btnEditPost.setOnClickListener(v -> showPostAuthorMenu(post, currentUserId));
        } else {
            btnEditPost.setVisibility(View.GONE);
        }

        // Like status
        interactionService.isPostLiked(currentUserId, post.getId(), new InteractionService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isLiked) {
                if (isLiked) {
                    btnLike.setText("Đã thích");
                    btnLike.setTextColor(0xFFE53935);
                } else {
                    btnLike.setText("Thích");
                    btnLike.setTextColor(0xFF000000);
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        // Save status
        postService.isPostSaved(currentUserId, post.getId(), new PostService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isSaved) {
                if (isSaved) {
                    btnSave.setText("Đã lưu");
                    btnSave.setTextColor(0xFF2E7D32);
                } else {
                    btnSave.setText("Lưu");
                    btnSave.setTextColor(0xFF000000);
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        // Like button listener
        btnLike.setOnClickListener(v -> {
            boolean wasLiked = btnLike.getText().toString().equals("Đã thích");
            if (wasLiked) {
                interactionService.unlikePost(currentUserId, post.getId(), new InteractionService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
                        btnLike.setText("Thích");
                        btnLike.setTextColor(0xFF000000);
                        tvLikesCount.setText(post.getLikesCount() + " lượt thích");
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(PostDetailActivity.this, "Lỗi thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                interactionService.likePost(currentUserId, post.getId(), new InteractionService.Callback<Like>() {
                    @Override
                    public void onSuccess(Like result) {
                        post.setLikesCount(post.getLikesCount() + 1);
                        btnLike.setText("Đã thích");
                        btnLike.setTextColor(0xFFE53935);
                        tvLikesCount.setText(post.getLikesCount() + " lượt thích");

                        notificationService.createNotificationAsync(post.getAuthorId(), currentUserId, currentUserId, "LIKE", post.getId());
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(PostDetailActivity.this, "Lỗi thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Save button listener
        btnSave.setOnClickListener(v -> {
            boolean wasSaved = btnSave.getText().toString().equals("Đã lưu");
            if (wasSaved) {
                postService.unsavePost(currentUserId, post.getId(), new PostService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        btnSave.setText("Lưu");
                        btnSave.setTextColor(0xFF000000);
                        Toast.makeText(PostDetailActivity.this, "Đã bỏ lưu bài viết", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(PostDetailActivity.this, "Lỗi bỏ lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                postService.savePost(currentUserId, post.getId(), new PostService.Callback<SavedPost>() {
                    @Override
                    public void onSuccess(SavedPost result) {
                        btnSave.setText("Đã lưu");
                        btnSave.setTextColor(0xFF2E7D32);
                        Toast.makeText(PostDetailActivity.this, "Đã lưu bài viết", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(PostDetailActivity.this, "Lỗi lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Comment button focuses comment field
        btnComment.setOnClickListener(v -> etCommentInput.requestFocus());
    }

    private void showPostAuthorMenu(Post post, String currentUserId) {
        List<String> options = new ArrayList<>();
        options.add("Sửa bài viết");
        options.add("Xóa bài viết");
        options.add("Lưu ảnh về thiết bị");

        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn bài viết")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String choice = options.get(which);
                    if (choice.equals("Sửa bài viết")) {
                        showEditDialog(post, currentUserId);
                    } else if (choice.equals("Xóa bài viết")) {
                        showDeleteConfirmDialog(post, currentUserId);
                    } else if (choice.equals("Lưu ảnh về thiết bị")) {
                        postService.saveImageToDevice(this, post.getImageUrl(), new PostService.Callback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                Toast.makeText(PostDetailActivity.this, result, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(PostDetailActivity.this, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .show();
    }

    private void showEditDialog(Post post, String currentUserId) {
        final EditText input = new EditText(this);
        input.setText(post.getCaption());
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Sửa bài viết")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newCaption = input.getText().toString().trim();
                    postService.updatePost(currentUserId, post.getId(), newCaption, new PostService.Callback<Post>() {
                        @Override
                        public void onSuccess(Post updatedPost) {
                            post.setCaption(newCaption);
                            tvCaption.setText((post.getAuthorUsername() != null ? post.getAuthorUsername() : post.getAuthorId()) + " " + newCaption);
                            Toast.makeText(PostDetailActivity.this, "Đã cập nhật bài viết!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(PostDetailActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog(Post post, String currentUserId) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    postService.deletePost(currentUserId, post.getId(), new PostService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(PostDetailActivity.this, "Đã xóa bài viết!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(PostDetailActivity.this, "Lỗi xóa bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadComments() {
        progressBarComments.setVisibility(View.VISIBLE);
        tvNoComments.setVisibility(View.GONE);

        interactionService.getComments(postId, new InteractionService.Callback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> comments) {
                progressBarComments.setVisibility(View.GONE);
                if (comments.isEmpty()) {
                    tvNoComments.setVisibility(View.VISIBLE);
                } else {
                    tvNoComments.setVisibility(View.GONE);
                }
                commentAdapter.setComments(comments);
            }

            @Override
            public void onError(Exception e) {
                progressBarComments.setVisibility(View.GONE);
                tvNoComments.setVisibility(View.VISIBLE);
                tvNoComments.setText("Chưa có bình luận nào");
            }
        });
    }

    private void handleSendComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String currentUserId = authService.getCurrentUserId();
        etCommentInput.setText("");

        interactionService.addComment(currentUserId, currentUserId, "", postId, text, new InteractionService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment comment) {
                tvNoComments.setVisibility(View.GONE);
                commentAdapter.addComment(comment);
                if (currentPost != null) {
                    notificationService.createNotificationAsync(currentPost.getAuthorId(), currentUserId, currentUserId, "COMMENT", postId);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(PostDetailActivity.this, "Lỗi gửi bình luận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
