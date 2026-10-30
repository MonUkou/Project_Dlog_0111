package com.example.myapplication.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.PhotoViewerActivity;
import com.example.myapplication.CreatePostActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.Comment;
import com.example.myapplication.model.SavedPost;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.ChatService;
import com.example.myapplication.service.impl.InteractionService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.service.impl.OfflinePages;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.UiMotion;
import com.example.myapplication.util.PostDisplayText;
import com.example.myapplication.view.PhotoCollageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> posts = new ArrayList<>();
    private final PostService postService;
    private final InteractionService interactionService;
    private final AuthService authService;
    private final OfflinePages offlinePages;
    private OnPostChangeListener onPostChangeListener;
    private int lastAnimatedPosition = -1;
    private final Set<String> expandedCaptionIds = new HashSet<>();
    private final Set<String> expandedCommentIds = new HashSet<>();
    private final Map<String, String> currentAuthorAvatars = new HashMap<>();
    private final Map<String, String> currentAuthorCodes = new HashMap<>();
    private final Set<String> pendingAuthorAvatars = new HashSet<>();
    private boolean adminViewer;

    public interface OnPostChangeListener {
        void onPostUpdated();
        void onPostDeleted(String postId);
    }

    public PostAdapter(Context context) {
        this.context = context;
        this.postService = PostService.getInstance();
        this.authService = AuthService.getInstance();
        this.interactionService = new InteractionService(authService, null);
        this.offlinePages = new OfflinePages(context);
        loadViewerRole();
    }

    public void setOnPostChangeListener(OnPostChangeListener listener) {
        this.onPostChangeListener = listener;
    }

    public void setPosts(List<Post> newPosts) {
        this.posts.clear();
        expandedCommentIds.clear();
        lastAnimatedPosition = -1;
        if (newPosts != null) {
            for (Post post : newPosts) {
                if (post != null && !post.isHidden()) this.posts.add(post);
            }
        }
        notifyDataSetChanged();
    }

    public void addPosts(List<Post> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPos = this.posts.size();
            for (Post post : newPosts) {
                if (post != null && !post.isHidden()) this.posts.add(post);
            }
            int insertedCount = this.posts.size() - startPos;
            if (insertedCount > 0) notifyItemRangeInserted(startPos, insertedCount);
        }
    }

    /** Applies Firestore changes to a visible post, including removing moderated posts. */
    public void updatePostRealtime(Post updatedPost) {
        if (updatedPost == null || updatedPost.getId() == null) return;
        for (int i = 0; i < posts.size(); i++) {
            if (updatedPost.getId().equals(posts.get(i).getId())) {
                if (updatedPost.isHidden()) {
                    posts.remove(i);
                    expandedCommentIds.remove(updatedPost.getId());
                    notifyItemRemoved(i);
                    return;
                }
                Post current = posts.get(i);
                current.setLikesCount(updatedPost.getLikesCount());
                current.setCommentsCount(updatedPost.getCommentsCount());
                notifyItemChanged(i);
                return;
            }
        }
    }

    /** Restores a post after an administrator makes it visible again. */
    public void restorePostRealtime(Post restoredPost) {
        if (restoredPost == null || restoredPost.getId() == null || restoredPost.isHidden()) return;
        for (Post post : posts) {
            if (restoredPost.getId().equals(post.getId())) {
                updatePostRealtime(restoredPost);
                return;
            }
        }

        String viewerId = authService.getCurrentUserId();
        boolean ownPost = viewerId != null && viewerId.equals(restoredPost.getAuthorId());
        long age = System.currentTimeMillis() - restoredPost.getCreatedAt();
        if (ownPost && (age < 0 || age >= com.example.myapplication.util.FeedPageSelector.OWN_POST_VISIBILITY_MS)) {
            return;
        }

        int insertAt = 0;
        while (insertAt < posts.size()
                && posts.get(insertAt).getCreatedAt() > restoredPost.getCreatedAt()) {
            insertAt++;
        }
        posts.add(insertAt, restoredPost);
        notifyItemInserted(insertAt);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        stopInlineCommentObserver(holder);
        Post post = posts.get(position);
        final List<String> photos = post.getImageUrls() != null && !post.getImageUrls().isEmpty()
                ? post.getImageUrls() : java.util.Collections.singletonList(post.getImageUrl());
        holder.photoCollage.show(photos, new PhotoCollageView.OnPhotoClickListener() {
            @Override public void onPhotoClick(int index) {
                PhotoViewerActivity.open(context, post, index);
            }
        });
        holder.itemView.setTag(post.getId());
        holder.btnLike.setEnabled(true);
        String currentUserId = authService.getCurrentUserId();

        // Author username & avatar
        String username = post.getAuthorUsername() != null ? post.getAuthorUsername() : post.getAuthorId();
        holder.tvAuthorUsername.setText(username);
        String authorId = post.getAuthorId();
        String timeText = new SimpleDateFormat("HH:mm · dd/MM/yyyy", new Locale("vi", "VN"))
                .format(new Date(post.getCreatedAt()));
        if (adminViewer) {
            String postCode = post.getPostCode();
            String userCode = currentAuthorCodes.get(authorId);
            if (postCode != null && !postCode.trim().isEmpty()) timeText += " · " + postCode;
            if (userCode != null && !userCode.trim().isEmpty()) timeText += " · " + userCode;
        }
        holder.tvPostTime.setText(timeText);

        String avatarUrl = currentAuthorAvatars.containsKey(authorId)
                ? currentAuthorAvatars.get(authorId) : post.getAuthorAvatarUrl();
        bindAuthorAvatar(holder.ivAuthorAvatar, avatarUrl);
        refreshAuthorAvatar(authorId);

        // Likes count & caption
        holder.btnLike.setText(String.valueOf(post.getLikesCount()));

        String captionText = PostDisplayText.captionWithTags(post.getCaption(), post.getHashtags());
        bindCaption(holder, post, captionText);
        holder.btnComment.setText(String.valueOf(post.getCommentsCount()));
        holder.btnComment.setContentDescription("Bình luận, " + post.getCommentsCount());
        holder.btnLike.setSelected(false);
        holder.btnSave.setSelected(false);

        // Check if current user liked post
        interactionService.isLiked(post.getId(), new InteractionService.ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isLiked) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                if (isLiked) {
                    holder.btnLike.setSelected(true);
                    holder.btnLike.setContentDescription("Bỏ thích bài viết, " + post.getLikesCount());
                    holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_error));
                } else {
                    holder.btnLike.setSelected(false);
                    holder.btnLike.setContentDescription("Thích bài viết, " + post.getLikesCount());
                    holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                }
            }

            @Override
            public void onError(String message) {}
        });

        // Check if current user saved post
        postService.isPostSaved(currentUserId, post.getId(), new PostService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isSaved) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                if (isSaved) {
                    holder.btnSave.setSelected(true);
                    holder.btnSave.setContentDescription("Bỏ lưu bài viết");
                    holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_success));
                } else {
                    holder.btnSave.setSelected(false);
                    holder.btnSave.setContentDescription("Lưu bài viết");
                    holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        // Click Like button
        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean wasLiked = holder.btnLike.isSelected();
                final long previousLikesCount = post.getLikesCount();
                holder.btnLike.setEnabled(false);
                if (wasLiked) {
                    post.setLikesCount(Math.max(0, previousLikesCount - 1));
                    holder.btnLike.setSelected(false);
                    holder.btnLike.setContentDescription("Thích bài viết, " + post.getLikesCount());
                    holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                    holder.btnLike.setText(String.valueOf(post.getLikesCount()));
                    interactionService.unlike(post.getId(), new InteractionService.ServiceCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!post.getId().equals(holder.itemView.getTag())) return;
                            holder.btnLike.setEnabled(true);
                        }

                    @Override
                    public void onError(String message) {
                        post.setLikesCount(previousLikesCount);
                        holder.btnLike.setSelected(true);
                        holder.btnLike.setText(String.valueOf(previousLikesCount));
                        holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_error));
                        holder.btnLike.setEnabled(true);
                        Toast.makeText(context, "Lỗi thao tác thích: " + message, Toast.LENGTH_SHORT).show();
                    }
                    });
                } else {
                    post.setLikesCount(previousLikesCount + 1);
                    holder.btnLike.setSelected(true);
                    holder.btnLike.setContentDescription("Bỏ thích bài viết, " + post.getLikesCount());
                    holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_error));
                    holder.btnLike.setText(String.valueOf(post.getLikesCount()));
                    interactionService.like(post.getId(), new InteractionService.ServiceCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!post.getId().equals(holder.itemView.getTag())) return;
                            holder.btnLike.setEnabled(true);

                        }

                    @Override
                    public void onError(String message) {
                        post.setLikesCount(previousLikesCount);
                        holder.btnLike.setSelected(false);
                        holder.btnLike.setText(String.valueOf(previousLikesCount));
                        holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                        holder.btnLike.setEnabled(true);
                        Toast.makeText(context, "Lỗi thao tác thích: " + message, Toast.LENGTH_SHORT).show();
                    }
                        });
                }
            }
        });

        // Click Save button (F21: Save, F22: Unsave)
        holder.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean wasSaved = holder.btnSave.isSelected();
                if (wasSaved) {
                    holder.btnSave.setEnabled(false);
                    postService.unsavePost(currentUserId, post.getId(), new PostService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            holder.btnSave.setEnabled(true);
                            if (!post.getId().equals(holder.itemView.getTag())) return;
                            holder.btnSave.setSelected(false);
                            holder.btnSave.setContentDescription("Lưu bài viết");
                            holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                            Toast.makeText(context, "Đã bỏ lưu bài viết", Toast.LENGTH_SHORT).show();
                        }

                    @Override
                    public void onError(Exception e) {
                        holder.btnSave.setEnabled(true);
                        Toast.makeText(context, "Lỗi bỏ lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    });
                } else {
                    savePostDirectly(holder, post, currentUserId);
                }
            }
        });

        boolean commentsExpanded = expandedCommentIds.contains(post.getId());
        holder.commentsPanel.setVisibility(commentsExpanded ? View.VISIBLE : View.GONE);
        holder.btnComment.setSelected(commentsExpanded);
        holder.btnComment.setContentDescription(commentsExpanded
                ? "Đóng bình luận" : "Mở bình luận, " + post.getCommentsCount());
        if (commentsExpanded) {
            loadInlineComments(holder, post);
        } else {
            stopInlineCommentObserver(holder);
            holder.progressInlineComments.setVisibility(View.GONE);
            holder.tvCommentsEmpty.setVisibility(View.GONE);
            holder.tvCommentsCacheStatus.setVisibility(View.GONE);
            holder.commentAdapter.setComments(null);
        }

        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (expandedCommentIds.contains(post.getId())) {
                    expandedCommentIds.remove(post.getId());
                } else {
                    expandedCommentIds.add(post.getId());
                }
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(adapterPosition);
                }
            }
        });
        holder.btnSendInlineComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitInlineComment(holder, post);
            }
        });
        holder.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sharePost(post);
            }
        });

        // Click author header -> open ProfileActivity
        View.OnClickListener openProfileListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!com.example.myapplication.util.NetworkState.isOnline(context)
                        && !offlinePages.hasProfile(post.getAuthorId())) {
                    Toast.makeText(context,
                            "Hồ sơ này chưa được mở trong phiên khi có mạng.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", post.getAuthorId());
                context.startActivity(intent);
            }
        };
        holder.llAuthorHeader.setOnClickListener(openProfileListener);

        // Long press post for author actions.
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return false;
                showPostOptionsDialog(post, currentUserId, adapterPosition);
                return true;
            }
        });
        holder.itemView.findViewById(R.id.tvPostOptions).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;
                showPostOptionsDialog(post, currentUserId, adapterPosition);
            }
        });
        int adapterPosition = holder.getBindingAdapterPosition();
        if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition > lastAnimatedPosition) {
            UiMotion.reveal(holder.itemView, adapterPosition);
            lastAnimatedPosition = adapterPosition;
        }
    }

    private void bindAuthorAvatar(ImageView avatar, String url) {
        if (url == null || url.trim().isEmpty()) {
            Glide.with(context).clear(avatar);
            avatar.setImageResource(R.drawable.bg_circle_avatar);
        } else {
            Glide.with(context).load(url.trim())
                    .placeholder(R.drawable.bg_circle_avatar)
                    .error(R.drawable.bg_circle_avatar).circleCrop().into(avatar);
        }
    }

    private void refreshAuthorAvatar(String authorId) {
        if (authorId == null || authorId.isEmpty()
                || (currentAuthorAvatars.containsKey(authorId) && currentAuthorCodes.containsKey(authorId))
                || !pendingAuthorAvatars.add(authorId)) return;
        FirebaseFirestore.getInstance().collection("users").document(authorId).get()
                .addOnSuccessListener(document -> {
                    pendingAuthorAvatars.remove(authorId);
                    String url = document.getString("avatarUrl");
                    String code = document.getString("userCode");
                    currentAuthorAvatars.put(authorId, url == null ? "" : url);
                    currentAuthorCodes.put(authorId, code == null ? "" : code);
                    for (int i = 0; i < posts.size(); i++) {
                        if (authorId.equals(posts.get(i).getAuthorId())) notifyItemChanged(i);
                    }
                })
                .addOnFailureListener(error -> pendingAuthorAvatars.remove(authorId));
    }

    private void loadViewerRole() {
        String viewerId = authService.getCurrentUserId();
        if (viewerId == null || viewerId.trim().isEmpty()) return;
        FirebaseFirestore.getInstance().collection("users").document(viewerId).get()
                .addOnSuccessListener(document -> {
                    boolean isAdmin = "admin".equalsIgnoreCase(document.getString("role"));
                    if (adminViewer != isAdmin) {
                        adminViewer = isAdmin;
                        notifyDataSetChanged();
                    }
                });
    }

    private void loadInlineComments(final PostViewHolder holder, final Post post) {
        stopInlineCommentObserver(holder);
        final boolean hasCache = offlinePages.hasComments(post.getId());
        final List<Comment> cachedComments = offlinePages.comments(post.getId());
        if (hasCache) {
            holder.commentAdapter.setComments(cachedComments);
            holder.tvCommentsCacheStatus.setText("SQLite · Đang hiển thị bình luận đã lưu và kiểm tra dữ liệu mới…");
            holder.tvCommentsCacheStatus.setVisibility(View.GONE);
            holder.tvCommentsEmpty.setVisibility(cachedComments.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            holder.tvCommentsCacheStatus.setVisibility(View.GONE);
        }
        holder.progressInlineComments.setVisibility(View.VISIBLE);
        if (!hasCache) holder.tvCommentsEmpty.setVisibility(View.GONE);
        if (!com.example.myapplication.util.NetworkState.isOnline(context)) {
            holder.progressInlineComments.setVisibility(View.GONE);
            if (hasCache) {
                holder.commentAdapter.setComments(cachedComments);
                holder.tvCommentsEmpty.setVisibility(cachedComments.isEmpty()
                        ? View.VISIBLE : View.GONE);
            } else {
                holder.tvCommentsEmpty.setText("Chưa từng mở bình luận này trong phiên khi có mạng.");
                holder.tvCommentsEmpty.setVisibility(View.VISIBLE);
            }
            return;
        }
        holder.commentsRegistration = interactionService.observeComments(post.getId(),
                new InteractionService.ServiceCallback<List<Comment>>() {
                    @Override
                    public void onSuccess(List<Comment> comments) {
                        if (!post.getId().equals(holder.itemView.getTag())
                                || !expandedCommentIds.contains(post.getId())) return;
                        holder.progressInlineComments.setVisibility(View.GONE);
                        holder.tvCommentsCacheStatus.setVisibility(View.GONE);
                        holder.commentAdapter.setComments(comments);
                        offlinePages.saveComments(post.getId(), comments);
                        boolean empty = comments == null || comments.isEmpty();
                        holder.tvCommentsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        if (!empty) {
                            holder.rvInlineComments.scrollToPosition(comments.size() - 1);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!post.getId().equals(holder.itemView.getTag())) return;
                        holder.progressInlineComments.setVisibility(View.GONE);
                        if (hasCache) {
                            holder.commentAdapter.setComments(cachedComments);
                            holder.tvCommentsCacheStatus.setText("Ngoại tuyến · Bình luận đang đọc từ SQLite");
                            holder.tvCommentsCacheStatus.setVisibility(View.GONE);
                            holder.tvCommentsEmpty.setVisibility(cachedComments.isEmpty()
                                    ? View.VISIBLE : View.GONE);
                        } else {
                            holder.tvCommentsCacheStatus.setVisibility(View.GONE);
                            holder.tvCommentsEmpty.setText("Chưa từng mở bình luận này khi có mạng.");
                            holder.tvCommentsEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void stopInlineCommentObserver(PostViewHolder holder) {
        if (holder.commentsRegistration != null) {
            holder.commentsRegistration.remove();
            holder.commentsRegistration = null;
        }
    }

    private void submitInlineComment(final PostViewHolder holder, final Post post) {
        final String content = holder.etInlineComment.getText().toString().trim();
        if (content.isEmpty()) {
            holder.etInlineComment.setError("Hãy nhập nội dung bình luận");
            holder.etInlineComment.requestFocus();
            return;
        }
        final long previousCommentsCount = post.getCommentsCount();
        post.setCommentsCount(previousCommentsCount + 1L);
        holder.btnComment.setText(String.valueOf(post.getCommentsCount()));
        holder.btnComment.setContentDescription("Đóng bình luận");
        holder.btnSendInlineComment.setEnabled(false);
        interactionService.addComment(post.getId(), content,
                new InteractionService.ServiceCallback<Comment>() {
                    @Override
                    public void onSuccess(Comment comment) {
                        if (!post.getId().equals(holder.itemView.getTag())) return;
                        holder.btnSendInlineComment.setEnabled(true);
                        holder.etInlineComment.setText("");
                    }

                    @Override
                    public void onError(String message) {
                        post.setCommentsCount(previousCommentsCount);
                        if (post.getId().equals(holder.itemView.getTag())) {
                            holder.btnComment.setText(String.valueOf(previousCommentsCount));
                        }
                        holder.btnSendInlineComment.setEnabled(true);
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void savePostDirectly(final PostViewHolder holder, final Post post,
                                  final String currentUserId) {
        holder.btnSave.setEnabled(false);
        postService.savePost(currentUserId, post.getId(), new PostService.Callback<SavedPost>() {
            @Override
            public void onSuccess(SavedPost result) {
                holder.btnSave.setEnabled(true);
                if (!post.getId().equals(holder.itemView.getTag())) return;
                holder.btnSave.setSelected(true);
                holder.btnSave.setContentDescription("Bỏ lưu bài viết");
                holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_success));
                Toast.makeText(context, "Đã lưu vào mục Đã lưu", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                holder.btnSave.setEnabled(true);
                Toast.makeText(context, "Không thể lưu: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        stopInlineCommentObserver(holder);
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        super.onViewRecycled(holder);
    }

    private void bindCaption(final PostViewHolder holder, final Post post, String captionText) {
        final String text = captionText == null ? "" : captionText.trim();
        if (text.isEmpty()) {
            holder.captionRow.setVisibility(View.GONE);
            holder.btnCaptionMore.setOnClickListener(null);
            return;
        }

        holder.captionRow.setVisibility(View.VISIBLE);
        holder.tvCaption.setText(text);
        final boolean expanded = expandedCaptionIds.contains(post.getId());
        holder.tvCaption.setMaxLines(expanded ? Integer.MAX_VALUE : 1);
        holder.tvCaption.setEllipsize(expanded ? null : android.text.TextUtils.TruncateAt.END);
        holder.btnCaptionMore.setText(expanded ? "Thu gọn" : "Xem thêm");
        holder.btnCaptionMore.setContentDescription(expanded
                ? "Thu gọn mô tả bài viết" : "Xem đầy đủ mô tả bài viết");
        holder.btnCaptionMore.setVisibility(expanded ? View.VISIBLE : View.GONE);

        if (!expanded) {
            holder.tvCaption.post(new Runnable() {
                @Override
                public void run() {
                    if (!post.getId().equals(holder.itemView.getTag())) return;
                    Layout layout = holder.tvCaption.getLayout();
                    boolean truncated = layout != null
                            && layout.getLineCount() > 0
                            && layout.getEllipsisCount(layout.getLineCount() - 1) > 0;
                    holder.btnCaptionMore.setVisibility(truncated ? View.VISIBLE : View.GONE);
                }
            });
        }

        holder.btnCaptionMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (expandedCaptionIds.contains(post.getId())) {
                    expandedCaptionIds.remove(post.getId());
                } else {
                    expandedCaptionIds.add(post.getId());
                }
                bindCaption(holder, post, text);
            }
        });
    }

    private void showPostOptionsDialog(Post post, String currentUserId, int position) {
        boolean isAuthor = currentUserId != null && currentUserId.equals(post.getAuthorId());
        List<String> options = new ArrayList<>();
        options.add("Xem ảnh / báo cáo");
        if (isAuthor) {
            options.add("Sửa bài viết");
            options.add("Xóa bài viết");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tùy chọn bài viết");
        builder.setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selected = options.get(which);
                if (selected.equals("Xem ảnh / báo cáo")) {
                    PhotoViewerActivity.open(context, post, 0);
                } else if (selected.equals("Sửa bài viết")) {
                    Intent edit = new Intent(context, CreatePostActivity.class);
                    edit.putExtra(CreatePostActivity.EXTRA_EDIT_POST_ID, post.getId());
                    context.startActivity(edit);
                } else if (selected.equals("Xóa bài viết")) {
                    showDeletePostConfirmDialog(post, position);
                }
            }
        });
        builder.show();
    }

    private void sharePost(Post post) {
        String[] choices = {"Gửi qua Hộp thư DLog", "Chia sẻ qua ứng dụng khác"};
        new AlertDialog.Builder(context)
                .setTitle("Chia sẻ bài viết")
                .setItems(choices, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) showDlogShareRecipients(post);
                        else sharePostExternally(post);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDlogShareRecipients(final Post post) {
        if (!com.example.myapplication.util.NetworkState.isOnline(context)) {
            Toast.makeText(context, "Hộp thư cần kết nối mạng.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String currentUserId = authService.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressBar progress = new ProgressBar(context);
        progress.setPadding(dp(24), dp(16), dp(24), dp(16));
        final AlertDialog loading = new AlertDialog.Builder(context)
                .setTitle("Đang tải người bạn theo dõi…")
                .setView(progress)
                .setCancelable(false)
                .create();
        loading.show();

        UserService.getInstance().getFollowUsers(currentUserId, true,
                new UserService.Callback<List<User>>() {
                    @Override public void onSuccess(List<User> users) {
                        loading.dismiss();
                        if (users == null || users.isEmpty()) {
                            Toast.makeText(context,
                                    "Bạn chưa theo dõi ai để gửi bài viết.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final List<User> recipients = new ArrayList<>();
                        final List<String> labels = new ArrayList<>();
                        for (User user : users) {
                            if (user == null || user.getId() == null
                                    || currentUserId.equals(user.getId())) continue;
                            recipients.add(user);
                            String name = user.getFullName();
                            if (name == null || name.trim().isEmpty()) name = user.getUsername();
                            labels.add(name == null || name.trim().isEmpty()
                                    ? "Người dùng DLog" : name.trim());
                        }
                        if (recipients.isEmpty()) {
                            Toast.makeText(context,
                                    "Bạn chưa theo dõi ai để gửi bài viết.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new AlertDialog.Builder(context)
                                .setTitle("Gửi bài viết đến")
                                .setItems(labels.toArray(new String[0]),
                                        new DialogInterface.OnClickListener() {
                                            @Override public void onClick(DialogInterface dialog, int which) {
                                                sendPostToRecipient(post, recipients.get(which), currentUserId);
                                            }
                                        })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }

                    @Override public void onError(Exception error) {
                        loading.dismiss();
                        Toast.makeText(context,
                                "Không tải được danh sách đang theo dõi.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPostToRecipient(final Post post, User recipient, String currentUserId) {
        Toast.makeText(context, "Đang gửi bài viết…", Toast.LENGTH_SHORT).show();
        ChatService.getInstance().getOrCreateConversation(currentUserId, recipient,
                new ChatService.Callback<String>() {
                    @Override public void onSuccess(String conversationId) {
                        com.google.firebase.auth.FirebaseUser account =
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        String senderName = account == null ? null : account.getDisplayName();
                        if ((senderName == null || senderName.trim().isEmpty())
                                && account != null && account.getEmail() != null) {
                            senderName = account.getEmail().split("@")[0];
                        }
                        ChatService.getInstance().sendPost(conversationId, currentUserId,
                                senderName, post.getId(), post.getCaption(),
                                new ChatService.Callback<Void>() {
                                    @Override public void onSuccess(Void unused) {
                                        Toast.makeText(context,
                                                "Đã gửi bài viết cho " + displayName(recipient),
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override public void onError(Exception error) {
                                        Toast.makeText(context,
                                                "Không gửi được bài viết: " + error.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override public void onError(Exception error) {
                        Toast.makeText(context,
                                "Không mở được cuộc trò chuyện: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String displayName(User user) {
        if (user == null) return "người nhận";
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername().trim();
        }
        return "người nhận";
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void sharePostExternally(Post post) {
        String caption = post.getCaption() == null ? "" : post.getCaption().trim();
        String imageUrl = post.getImageUrl() == null ? "" : post.getImageUrl().trim();
        String text = (caption.isEmpty() ? "Xem bài viết này trên DLog" : caption)
                + (imageUrl.isEmpty() ? "" : "\n" + imageUrl);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ bài viết"));
    }

    private void showDeletePostConfirmDialog(Post post, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này?")
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postService.deletePost(authService.getCurrentUserId(), post.getId(), new PostService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (position >= 0 && position < posts.size()) {
                                posts.remove(position);
                                notifyItemRemoved(position);
                            }
                            Toast.makeText(context, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                            if (onPostChangeListener != null) onPostChangeListener.onPostDeleted(post.getId());
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(context, "Lỗi xóa bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        View llAuthorHeader;
        ImageView ivAuthorAvatar;
        TextView tvAuthorUsername;
        TextView tvPostTime;
        PhotoCollageView photoCollage;
        TextView btnLike;
        TextView btnComment;
        TextView btnShare;
        TextView btnSave;
        TextView tvLikesCount;
        TextView tvCaption;
        View captionRow;
        TextView btnCaptionMore;
        View commentsPanel;
        RecyclerView rvInlineComments;
        ProgressBar progressInlineComments;
        TextView tvCommentsEmpty;
        TextView tvCommentsCacheStatus;
        EditText etInlineComment;
        TextView btnSendInlineComment;
        CommentAdapter commentAdapter;
        ListenerRegistration commentsRegistration;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            llAuthorHeader = itemView.findViewById(R.id.llAuthorHeader);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorUsername = itemView.findViewById(R.id.tvAuthorUsername);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            photoCollage = itemView.findViewById(R.id.photoContainer);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnSave = itemView.findViewById(R.id.btnSave);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            captionRow = itemView.findViewById(R.id.captionRow);
            btnCaptionMore = itemView.findViewById(R.id.btnCaptionMore);
            commentsPanel = itemView.findViewById(R.id.commentsPanel);
            rvInlineComments = itemView.findViewById(R.id.rvInlineComments);
            progressInlineComments = itemView.findViewById(R.id.progressInlineComments);
            tvCommentsEmpty = itemView.findViewById(R.id.tvCommentsEmpty);
            tvCommentsCacheStatus = itemView.findViewById(R.id.tvCommentsCacheStatus);
            etInlineComment = itemView.findViewById(R.id.etInlineComment);
            btnSendInlineComment = itemView.findViewById(R.id.btnSendInlineComment);
            commentAdapter = new CommentAdapter(itemView.getContext());
            rvInlineComments.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rvInlineComments.setAdapter(commentAdapter);
        }
    }
}
