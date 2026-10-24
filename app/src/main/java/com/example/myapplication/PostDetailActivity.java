package com.example.myapplication;

import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.Comment;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.InteractionService;
import com.example.myapplication.service.impl.OfflinePages;
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
    private TextView btnSharePost;
    private ImageView ivDetailAuthorAvatar, ivDetailPostImage;
    private final List<String> photoUrls = new ArrayList<>();
    private int photoIndex = 0;
    private TextView tvImagePlaceholder;
    private Button btnSendComment;
    private EditText edtComment;
    private ProgressBar progressBarComments;
    private RecyclerView rvComments;
    private TextView tvOfflineDetail;
    private Post cachedPost;

    private InteractionService interactionService;
    private CommentAdapter commentAdapter;

    private String postId;
    private String currentAuthor = "";
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
            postId = getIntent().getStringExtra("postId");
        }
        if (postId == null || postId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã bài viết.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();

        cachedPost = new OfflinePages(this).post(postId);
        if (cachedPost != null) applyPost(cachedPost);

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
        btnSharePost = findViewById(R.id.btnSharePost);
        ivDetailAuthorAvatar = findViewById(R.id.viewAuthorAvatar);
        ivDetailPostImage = findViewById(R.id.ivPostImage);
        ivDetailPostImage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Post viewerPost = cachedPost;
                if (viewerPost == null) {
                    viewerPost = new Post();
                    viewerPost.setImageUrls(new ArrayList<String>(photoUrls));
                    if (!photoUrls.isEmpty()) viewerPost.setImageUrl(photoUrls.get(0));
                }
                PhotoViewerActivity.open(PostDetailActivity.this, viewerPost, photoIndex);
            }
        });
        findViewById(R.id.btnDownloadPhoto).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (photoUrls.isEmpty()) return;
                com.example.myapplication.service.impl.PostService.getInstance().saveImageToDevice(
                        PostDetailActivity.this, photoUrls.get(photoIndex),
                        new com.example.myapplication.service.impl.PostService.Callback<String>() {
                            @Override public void onSuccess(String message) {
                                runOnUiThread(new Runnable() { @Override public void run() {
                                    Toast.makeText(PostDetailActivity.this, message, Toast.LENGTH_LONG).show();
                                }});
                            }
                            @Override public void onError(Exception error) {
                                runOnUiThread(new Runnable() { @Override public void run() {
                                    Toast.makeText(PostDetailActivity.this, "Không thể tải ảnh: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }});
                            }
                        });
            }
        });
        tvImagePlaceholder = findViewById(R.id.tvImagePlaceholder);
        btnSendComment = findViewById(R.id.btnSendComment);
        edtComment = findViewById(R.id.edtComment);
        progressBarComments = findViewById(R.id.progressBarComments);
        rvComments = findViewById(R.id.rvComments);
        tvOfflineDetail = findViewById(R.id.tvOfflineDetail);

        commentAdapter = new CommentAdapter(this, new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onCommentClick(Comment comment) {
                showDeleteCommentDialog(comment);
            }
        });
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { handleLikeClick(); }
        });
        btnFocusComment.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { edtComment.requestFocus(); }
        });
        btnSendComment.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { handleSendComment(); }
        });
        btnSharePost.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { sharePost(); }
        });
    }

    private void loadPostDetails() {
        FirebaseFirestore.getInstance().collection("posts").document(postId).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override public void onSuccess(com.google.firebase.firestore.DocumentSnapshot doc) {
                    if (doc.exists() && !Boolean.TRUE.equals(doc.getBoolean("hidden"))) {
                        Post freshPost = Post.fromMap(doc.getData());
                        if (freshPost != null) {
                            freshPost.setId(doc.getId());
                            new OfflinePages(PostDetailActivity.this).savePost(freshPost);
                            cachedPost = freshPost;
                        }
                        String authorName = doc.getString("authorUsername");
                        String authorAvatarUrl = doc.getString("authorAvatarUrl");
                        String authorId = doc.getString("authorId");
                        String imageUrl = doc.getString("imageUrl");
                        String caption = doc.getString("caption");
                        Long likes = doc.getLong("likesCount");
                        Long comments = doc.getLong("commentsCount");
                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) {
                            ((TextView) findViewById(R.id.tvPostTime)).setText(
                                    new java.text.SimpleDateFormat("HH:mm · dd/MM/yyyy", new java.util.Locale("vi", "VN"))
                                            .format(new java.util.Date(createdAt)));
                        }

                        currentAuthor = (authorName != null && !authorName.isEmpty())
                                ? authorName : (authorId != null ? authorId : "Người dùng DLog");
                        currentCaption = (caption != null) ? caption : "";
                        Object tagValue = doc.get("hashtags");
                        if (tagValue instanceof List) {
                            List<String> tags = new ArrayList<>();
                            for (Object item : (List<?>) tagValue) {
                                if (item instanceof String) tags.add((String) item);
                            }
                            currentCaption = com.example.myapplication.util.PostDisplayText.captionWithTags(
                                    currentCaption, tags);
                        }
                        currentLikesCount = (likes != null) ? likes : 0;
                        currentCommentsCount = (comments != null) ? comments : 0;

                        if (authorAvatarUrl != null && !authorAvatarUrl.trim().isEmpty()) {
                            Glide.with(PostDetailActivity.this).load(authorAvatarUrl).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(ivDetailAuthorAvatar);
                        }
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            photoUrls.clear();
                            Object images = doc.get("imageUrls");
                            if (images instanceof List) {
                                for (Object item : (List<?>) images) if (item instanceof String) photoUrls.add((String) item);
                            }
                            if (photoUrls.isEmpty()) photoUrls.add(imageUrl);
                            photoIndex = 0;
                            showCurrentPhoto();
                        }

                        updatePostInfoUI();
                        updateCounterViews();
                    } else {
                        Toast.makeText(PostDetailActivity.this, "Bài viết đã được ẩn hoặc không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) {
                        if (cachedPost != null) {
                            showOfflineState();
                        } else {
                            Toast.makeText(PostDetailActivity.this,
                                    "Không có dữ liệu bài viết đã lưu trên máy.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void applyPost(Post post) {
        currentAuthor = post.getAuthorUsername() == null || post.getAuthorUsername().isEmpty()
                ? post.getAuthorId() : post.getAuthorUsername();
        currentCaption = com.example.myapplication.util.PostDisplayText.captionWithTags(
                post.getCaption(), post.getHashtags());
        currentLikesCount = post.getLikesCount();
        currentCommentsCount = post.getCommentsCount();
        ((TextView) findViewById(R.id.tvPostTime)).setText(
                new java.text.SimpleDateFormat("HH:mm · dd/MM/yyyy", new java.util.Locale("vi", "VN"))
                        .format(new java.util.Date(post.getCreatedAt())));
        if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
            Glide.with(this).load(post.getAuthorAvatarUrl()).placeholder(R.drawable.bg_circle_avatar)
                    .error(R.drawable.bg_circle_avatar).circleCrop().into(ivDetailAuthorAvatar);
        }
        photoUrls.clear();
        if (post.getImageUrls() != null) photoUrls.addAll(post.getImageUrls());
        if (photoUrls.isEmpty() && post.getImageUrl() != null) photoUrls.add(post.getImageUrl());
        photoIndex = 0;
        if (!photoUrls.isEmpty()) showCurrentPhoto();
        updatePostInfoUI();
        updateCounterViews();
    }

    private void showOfflineState() {
        tvOfflineDetail.setVisibility(View.VISIBLE);
        progressBarComments.setVisibility(View.GONE);
        tvEmptyComments.setText("Bình luận chưa được lưu ngoại tuyến. Hãy kết nối mạng để xem.");
        tvEmptyComments.setVisibility(View.VISIBLE);
        rvComments.setVisibility(View.GONE);
        edtComment.setEnabled(false);
        btnSendComment.setEnabled(false);
        btnFocusComment.setEnabled(false);
    }

    private void showCurrentPhoto() {
        if (photoUrls.isEmpty()) return;
        tvImagePlaceholder.setVisibility(View.GONE);
        Glide.with(this).load(photoUrls.get(photoIndex)).centerCrop().into(ivDetailPostImage);
        TextView position = findViewById(R.id.tvPhotoPosition);
        position.setVisibility(photoUrls.size() > 1 ? View.VISIBLE : View.GONE);
        position.setText((photoIndex + 1) + "/" + photoUrls.size());
        ivDetailPostImage.setContentDescription("Ảnh " + (photoIndex + 1) + " trong " + photoUrls.size() + " ảnh. Nhấn để xem ảnh kế tiếp.");
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
                tvOfflineDetail.setVisibility(View.GONE);
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
                showOfflineState();
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

    private void sharePost() {
        FirebaseFirestore.getInstance().collection("posts").document(postId).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override public void onSuccess(com.google.firebase.firestore.DocumentSnapshot document) {
                    if (!document.exists() || Boolean.TRUE.equals(document.getBoolean("hidden"))) {
                        Toast.makeText(PostDetailActivity.this, "Bài viết không còn tồn tại.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String caption = document.getString("caption");
                    String imageUrl = document.getString("imageUrl");
                    String shareText = (caption == null || caption.trim().isEmpty()
                            ? "Xem bài viết này trên DLog" : caption.trim())
                            + (imageUrl == null || imageUrl.trim().isEmpty() ? "" : "\n" + imageUrl);
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, shareText);
                    startActivity(Intent.createChooser(send, "Chia sẻ bài viết"));
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception error) {
                        Toast.makeText(PostDetailActivity.this,
                                "Không thể tải bài viết để chia sẻ.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDeleteCommentDialog(Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này không?")
                .setPositiveButton("Xóa", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
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
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updatePostInfoUI() {
        tvPostAuthor.setText(currentAuthor);
        tvPostCaption.setText(currentCaption);
        tvPostCaption.setVisibility(currentCaption.trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateLikeButtonUI() {
        if (isLiked) {
            btnLike.setSelected(true);
            btnLike.setContentDescription("Bỏ thích bài viết, " + currentLikesCount);
            btnLike.setTextColor(ContextCompat.getColor(this, R.color.dlog_error));
        } else {
            btnLike.setSelected(false);
            btnLike.setContentDescription("Thích bài viết, " + currentLikesCount);
            btnLike.setTextColor(ContextCompat.getColor(this, R.color.dlog_ink));
        }
    }

    private void updateCounterViews() {
        btnLike.setText(String.valueOf(currentLikesCount));
        btnLike.setContentDescription((isLiked ? "Bỏ thích" : "Thích") + " bài viết, " + currentLikesCount);
        btnFocusComment.setText(String.valueOf(currentCommentsCount));
        btnFocusComment.setContentDescription("Bình luận, " + currentCommentsCount);
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
        private final android.content.Context context;
        private final OnCommentClickListener listener;

        CommentAdapter(android.content.Context context, OnCommentClickListener listener) {
            this.context = context;
            this.listener = listener;
        }

        void setComments(List<Comment> list) {
            this.comments = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(parent.getContext());
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(parent, 7), 0, dp(parent, 7));
            ImageView avatar = new ImageView(parent.getContext());
            avatar.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(parent, 36), dp(parent, 36)));
            avatar.setBackgroundResource(R.drawable.bg_circle_avatar);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            TextView text = new TextView(parent.getContext());
            android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.setMarginStart(dp(parent, 9));
            text.setLayoutParams(textParams);
            text.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
            text.setTextSize(14f);
            row.addView(avatar);
            row.addView(text);
            return new CommentViewHolder(row, avatar, text);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment c = comments.get(position);
            String name = c.getUsername() != null && !c.getUsername().trim().isEmpty()
                    ? c.getUsername() : c.getAuthorId();
            holder.tvCommentLine.setText(formatBoldPrefix(name, c.getContent()));
            if (c.getAvatarUrl() != null && !c.getAvatarUrl().trim().isEmpty()) {
                Glide.with(context).load(c.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.bg_circle_avatar);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    listener.onCommentClick(c);
                }
            });
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvCommentLine;
            ImageView ivAvatar;

            CommentViewHolder(@NonNull View itemView, ImageView avatar, TextView text) {
                super(itemView);
                this.tvCommentLine = text;
                this.ivAvatar = avatar;
            }
        }

        private static int dp(View view, int value) {
            return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
