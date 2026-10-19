package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Comment;
import com.example.myapplication.service.impl.InteractionService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "POST_ID";
    private static final String STATE_COMMENT_DRAFT = "STATE_COMMENT_DRAFT";
    private static final String STATE_IS_LIKED = "STATE_IS_LIKED";
    private static final String STATE_LIKES_COUNT = "STATE_LIKES_COUNT";
    private static final String STATE_COMMENTS_COUNT = "STATE_COMMENTS_COUNT";
    private static final String STATE_AUTHOR = "STATE_AUTHOR";
    private static final String STATE_CAPTION = "STATE_CAPTION";

    private TextView tvBack, tvPostAuthor, tvPostCaption, tvLikesCount, tvCommentsCount, tvEmptyComments;
    private TextView btnLike, btnFocusComment;
    private Button btnSendComment;
    private EditText edtComment;
    private ProgressBar progressBarComments;
    private RecyclerView rvComments;

    private InteractionService interactionService;
    private CommentAdapter commentAdapter;

    private String postId;
    private String currentAuthor = "minh.anh";
    private String currentCaption = "";
    private boolean isLiked = false;
    private long currentLikesCount = 0;
    private long currentCommentsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        interactionService = new InteractionService();

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null || postId.trim().isEmpty()) {
            postId = "post_demo_1";
        }

        initViews();

        if (savedInstanceState != null) {
            edtComment.setText(savedInstanceState.getString(STATE_COMMENT_DRAFT, ""));
            isLiked = savedInstanceState.getBoolean(STATE_IS_LIKED, false);
            currentLikesCount = savedInstanceState.getLong(STATE_LIKES_COUNT, 0);
            currentCommentsCount = savedInstanceState.getLong(STATE_COMMENTS_COUNT, 0);
            currentAuthor = savedInstanceState.getString(STATE_AUTHOR, "minh.anh");
            currentCaption = savedInstanceState.getString(STATE_CAPTION, "");
            updatePostInfoUI();
            updateLikeButtonUI();
            updateCounterViews();
        }

        loadPostDetails();
        checkLikeStatus();
        loadCommentsList();
    }

    private void initViews() {
        tvBack = findViewById(R.id.tvBack);
        tvPostAuthor = findViewById(R.id.tvPostAuthor);
        tvPostCaption = findViewById(R.id.tvPostCaption);
        tvLikesCount = findViewById(R.id.tvLikesCount);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        btnLike = findViewById(R.id.btnLike);
        btnFocusComment = findViewById(R.id.btnFocusComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        edtComment = findViewById(R.id.edtComment);
        progressBarComments = findViewById(R.id.progressBarComments);
        rvComments = findViewById(R.id.rvComments);

        commentAdapter = new CommentAdapter(this::showDeleteCommentDialog);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        tvBack.setOnClickListener(v -> finish());
        btnLike.setOnClickListener(v -> handleLikeClick());
        btnFocusComment.setOnClickListener(v -> edtComment.requestFocus());
        btnSendComment.setOnClickListener(v -> handleSendComment());
    }

    private void loadPostDetails() {
        FirebaseFirestore.getInstance().collection("posts").document(postId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String authorId = doc.getString("authorId");
                        String caption = doc.getString("caption");
                        Long likes = doc.getLong("likesCount");
                        Long comments = doc.getLong("commentsCount");

                        currentAuthor = (authorId != null && !authorId.isEmpty()) ? authorId : "minh.anh";
                        currentCaption = (caption != null) ? caption : "";
                        currentLikesCount = (likes != null) ? likes : 0;
                        currentCommentsCount = (comments != null) ? comments : 0;

                        updatePostInfoUI();
                        updateCounterViews();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void checkLikeStatus() {
        interactionService.isLiked(postId, new InteractionService.ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                isLiked = Boolean.TRUE.equals(result);
                updateLikeButtonUI();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLikeClick() {
        btnLike.setEnabled(false);
        if (isLiked) {
            interactionService.unlike(postId, new InteractionService.ServiceCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isLiked = false;
                    currentLikesCount = Math.max(0, currentLikesCount - 1);
                    updateLikeButtonUI();
                    updateCounterViews();
                    btnLike.setEnabled(true);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    btnLike.setEnabled(true);
                }
            });
        } else {
            interactionService.like(postId, new InteractionService.ServiceCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isLiked = true;
                    currentLikesCount++;
                    updateLikeButtonUI();
                    updateCounterViews();
                    btnLike.setEnabled(true);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    btnLike.setEnabled(true);
                }
            });
        }
    }

    private void loadCommentsList() {
        progressBarComments.setVisibility(View.VISIBLE);
        tvEmptyComments.setVisibility(View.GONE);

        interactionService.getComments(postId, new InteractionService.ServiceCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
                progressBarComments.setVisibility(View.GONE);
                commentAdapter.setComments(result);
                currentCommentsCount = result.size();
                updateCounterViews();

                if (result.isEmpty()) {
                    tvEmptyComments.setVisibility(View.VISIBLE);
                    rvComments.setVisibility(View.GONE);
                } else {
                    tvEmptyComments.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                progressBarComments.setVisibility(View.GONE);
                Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSendComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) {
            edtComment.setError("Vui lòng nhập bình luận");
            return;
        }

        btnSendComment.setEnabled(false);
        interactionService.addComment(postId, content, new InteractionService.ServiceCallback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                edtComment.setText("");
                btnSendComment.setEnabled(true);
                loadCommentsList();
            }

            @Override
            public void onError(String message) {
                btnSendComment.setEnabled(true);
                Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteCommentDialog(Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    interactionService.deleteComment(comment.getId(), new InteractionService.ServiceCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(PostDetailActivity.this, "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                            loadCommentsList();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updatePostInfoUI() {
        tvPostAuthor.setText(currentAuthor);
        tvPostCaption.setText(formatBoldPrefix(currentAuthor, currentCaption));
    }

    private void updateLikeButtonUI() {
        if (isLiked) {
            btnLike.setText("Đã thích");
            btnLike.setTextColor(Color.parseColor("#E53935"));
        } else {
            btnLike.setText("Thích");
            btnLike.setTextColor(Color.parseColor("#111111"));
        }
    }

    private void updateCounterViews() {
        tvLikesCount.setText(currentLikesCount + " lượt thích");
        tvCommentsCount.setText(currentCommentsCount + " bình luận");
    }

    private static SpannableStringBuilder formatBoldPrefix(String author, String text) {
        String safeAuthor = (author != null) ? author : "";
        String safeText = (text != null && !text.isEmpty()) ? " " + text : "";
        SpannableStringBuilder builder = new SpannableStringBuilder(safeAuthor + safeText);
        if (!safeAuthor.isEmpty()) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, safeAuthor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (edtComment != null) {
            outState.putString(STATE_COMMENT_DRAFT, edtComment.getText().toString());
        }
        outState.putBoolean(STATE_IS_LIKED, isLiked);
        outState.putLong(STATE_LIKES_COUNT, currentLikesCount);
        outState.putLong(STATE_COMMENTS_COUNT, currentCommentsCount);
        outState.putString(STATE_AUTHOR, currentAuthor);
        outState.putString(STATE_CAPTION, currentCaption);
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

        interface OnCommentClickListener {
            void onCommentClick(Comment comment);
        }

        private List<Comment> comments = new ArrayList<>();
        private final OnCommentClickListener listener;

        CommentAdapter(OnCommentClickListener listener) {
            this.listener = listener;
        }

        void setComments(List<Comment> list) {
            this.comments = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tv.setPadding(0, 8, 0, 8);
            tv.setTextColor(Color.parseColor("#212121"));
            tv.setTextSize(14f);
            return new CommentViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment c = comments.get(position);
            holder.tvCommentLine.setText(formatBoldPrefix(c.getAuthorId(), c.getContent()));
            holder.itemView.setOnClickListener(v -> listener.onCommentClick(c));
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvCommentLine;

            CommentViewHolder(@NonNull TextView itemView) {
                super(itemView);
                this.tvCommentLine = itemView;
            }
        }
    }
}
