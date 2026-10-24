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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.PostDetailActivity;
import com.example.myapplication.PhotoViewerActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.InteractionService;
import com.example.myapplication.service.impl.PostService;
import com.example.myapplication.util.UiMotion;
import com.example.myapplication.util.PostDisplayText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> posts = new ArrayList<>();
    private final PostService postService;
    private final InteractionService interactionService;
    private final AuthService authService;
    private OnPostChangeListener onPostChangeListener;
    private int lastAnimatedPosition = -1;
    private final Set<String> expandedCaptionIds = new HashSet<>();

    public interface OnPostChangeListener {
        void onPostUpdated();
        void onPostDeleted(String postId);
    }

    public PostAdapter(Context context) {
        this.context = context;
        this.postService = PostService.getInstance();
        this.authService = AuthService.getInstance();
        this.interactionService = new InteractionService(authService, null);
    }

    public void setOnPostChangeListener(OnPostChangeListener listener) {
        this.onPostChangeListener = listener;
    }

    public void setPosts(List<Post> newPosts) {
        this.posts.clear();
        lastAnimatedPosition = -1;
        if (newPosts != null) {
            this.posts.addAll(newPosts);
        }
        notifyDataSetChanged();
    }

    public void addPosts(List<Post> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPos = this.posts.size();
            this.posts.addAll(newPosts);
            notifyItemRangeInserted(startPos, newPosts.size());
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        TextView photoCount = holder.itemView.findViewById(R.id.tvPhotoCount);
        final List<String> photos = post.getImageUrls() != null && !post.getImageUrls().isEmpty()
                ? post.getImageUrls() : java.util.Collections.singletonList(post.getImageUrl());
        final int totalPhotos = photos.size();
        holder.photoIndex = 0;
        photoCount.setVisibility(totalPhotos > 1 ? View.VISIBLE : View.GONE);
        holder.btnPreviousPhoto.setVisibility(View.GONE);
        holder.btnNextPhoto.setVisibility(totalPhotos > 1 ? View.VISIBLE : View.GONE);
        showPhoto(holder, photos, photoCount);
        holder.btnPreviousPhoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (holder.photoIndex > 0) {
                    holder.photoIndex--;
                    showPhoto(holder, photos, photoCount);
                }
            }
        });
        holder.btnNextPhoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (holder.photoIndex + 1 < totalPhotos) {
                    holder.photoIndex++;
                    showPhoto(holder, photos, photoCount);
                }
            }
        });
        holder.itemView.setTag(post.getId());
        holder.btnLike.setEnabled(true);
        String currentUserId = authService.getCurrentUserId();

        // Author username & avatar
        String username = post.getAuthorUsername() != null ? post.getAuthorUsername() : post.getAuthorId();
        holder.tvAuthorUsername.setText(username);
        holder.tvPostTime.setText(new SimpleDateFormat("HH:mm · dd/MM/yyyy", new Locale("vi", "VN"))
                .format(new Date(post.getCreatedAt())));

        if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(post.getAuthorAvatarUrl())
                    .placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop()
                    .into(holder.ivAuthorAvatar);
        } else {
            holder.ivAuthorAvatar.setImageResource(R.drawable.bg_circle_avatar);
        }

        // Likes count & caption
        holder.btnLike.setText(String.valueOf(post.getLikesCount()));

        String captionText = PostDisplayText.captionWithTags(post.getCaption(), post.getHashtags());
        bindCaption(holder, post, captionText);
        holder.btnComment.setText(String.valueOf(post.getCommentsCount()));
        holder.btnComment.setContentDescription("Bình luận, " + post.getCommentsCount());
        holder.btnLike.setSelected(false);
        holder.btnSave.setSelected(false);

        // Check if current user liked post
        setLikeUI(holder, false);
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
                holder.btnLike.setEnabled(false);
                if (wasLiked) {
                    interactionService.unlike(post.getId(), new InteractionService.ServiceCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
                        holder.btnLike.setSelected(false);
                    holder.btnLike.setContentDescription("Thích bài viết, " + post.getLikesCount());
                        holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                        holder.btnLike.setText(String.valueOf(post.getLikesCount()));
                        holder.btnLike.setEnabled(true);
                    }

                    @Override
                    public void onError(String message) {
                        holder.btnLike.setEnabled(true);
                        Toast.makeText(context, "Lỗi thao tác thích: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
                } else {
                    interactionService.like(post.getId(), new InteractionService.ServiceCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                        post.setLikesCount(post.getLikesCount() + 1);
                        holder.btnLike.setSelected(true);
                    holder.btnLike.setContentDescription("Bỏ thích bài viết, " + post.getLikesCount());
                        holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_error));
                        holder.btnLike.setText(String.valueOf(post.getLikesCount()));
                        holder.btnLike.setEnabled(true);
                    }

                    @Override
                    public void onError(String message) {
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
                    postService.unsavePost(currentUserId, post.getId(), new PostService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                        holder.btnSave.setSelected(false);
                    holder.btnSave.setContentDescription("Lưu bài viết");
                        holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
                        Toast.makeText(context, "Đã bỏ lưu bài viết", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Lỗi bỏ lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                } else {
                    showSaveCollectionDialog(holder, post, currentUserId);
                }
            }
        });

        // Click Comment button or post image -> open PostDetailActivity
        View.OnClickListener openDetailListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", post.getId());
                context.startActivity(intent);
            }
        };
        holder.btnComment.setOnClickListener(openDetailListener);
        holder.ivPostImage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                PhotoViewerActivity.open(context, post, holder.photoIndex);
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
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", post.getAuthorId());
                context.startActivity(intent);
            }
        };
        holder.llAuthorHeader.setOnClickListener(openProfileListener);

        // Long press post -> Option dialog (Save image to device F24, Edit/Delete if author)
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showPostOptionsDialog(post, currentUserId, position);
                return true;
            }
        });
        if (position > lastAnimatedPosition) {
            UiMotion.reveal(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    private void showSaveCollectionDialog(final PostViewHolder holder, final Post post, final String currentUserId) {
        final String[] collections = {"Chung", "Du lịch", "Nhiếp ảnh đường phố", "Tối giản"};
        new AlertDialog.Builder(context)
                .setTitle("Lưu vào bộ sưu tập")
                .setItems(collections, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        final String collection = collections[which];
                        postService.savePost(currentUserId, post.getId(), collection, new PostService.Callback<SavedPost>() {
                    @Override
                    public void onSuccess(SavedPost result) {
                if (!post.getId().equals(holder.itemView.getTag())) return;
                        holder.btnSave.setSelected(true);
                    holder.btnSave.setContentDescription("Bỏ lưu bài viết");
                        holder.btnSave.setTextColor(ContextCompat.getColor(context, R.color.dlog_success));
                                Toast.makeText(context, "Đã lưu vào “" + collection + "”", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Lỗi lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                        });
                    }
                }).show();
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        super.onViewRecycled(holder);
    }

    private void showPhoto(PostViewHolder holder, List<String> photos, TextView photoCount) {
        String url = photos.get(holder.photoIndex);
        StringBuilder indicator = new StringBuilder();
        for (int i = 0; i < photos.size(); i++) {
            if (i > 0) indicator.append(' ');
            indicator.append(i == holder.photoIndex ? '●' : '○');
        }
        photoCount.setText(indicator.toString());
        holder.btnPreviousPhoto.setVisibility(holder.photoIndex > 0 ? View.VISIBLE : View.GONE);
        holder.btnNextPhoto.setVisibility(holder.photoIndex + 1 < photos.size() ? View.VISIBLE : View.GONE);
        if (url == null || url.isEmpty()) {
            Glide.with(context).clear(holder.ivPostImage);
            holder.ivPostImage.setImageDrawable(null);
            holder.tvImagePlaceholder.setVisibility(View.VISIBLE);
        } else {
            holder.tvImagePlaceholder.setVisibility(View.GONE);
            Glide.with(context).load(url).centerCrop().into(holder.ivPostImage);
        }
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
        options.add("Lưu ảnh về thiết bị");
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
                if (selected.equals("Lưu ảnh về thiết bị")) {
                    postService.saveImageToDevice(context, post.getImageUrl(), new PostService.Callback<String>() {
                    @Override
                    public void onSuccess(String msg) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    });
                } else if (selected.equals("Sửa bài viết")) {
                    showEditPostDialog(post, position);
                } else if (selected.equals("Xóa bài viết")) {
                    showDeletePostConfirmDialog(post, position);
                }
            }
        });
        builder.show();
    }

    private void sharePost(Post post) {
        String caption = post.getCaption() == null ? "" : post.getCaption().trim();
        String imageUrl = post.getImageUrl() == null ? "" : post.getImageUrl().trim();
        String text = (caption.isEmpty() ? "Xem bài viết này trên DLog" : caption)
                + (imageUrl.isEmpty() ? "" : "\n" + imageUrl);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ bài viết"));
    }

    private void showEditPostDialog(Post post, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Sửa bài viết");

        final EditText input = new EditText(context);
        input.setText(post.getCaption());
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("Lưu", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String newCaption = input.getText().toString().trim();
                postService.updatePost(authService.getCurrentUserId(), post.getId(), newCaption, new PostService.Callback<Post>() {
                @Override
                public void onSuccess(Post updatedPost) {
                    post.setCaption(newCaption);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Đã cập nhật bài viết", Toast.LENGTH_SHORT).show();
                    if (onPostChangeListener != null) onPostChangeListener.onPostUpdated();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Lỗi sửa bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                });
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
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

    private void setLikeUI(PostViewHolder holder, boolean isLiked) {
        if (isLiked) {
            holder.btnLike.setText("Đã thích");
            holder.btnLike.setTextColor(0xFFE53935);
            holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_filled, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(holder.btnLike, ColorStateList.valueOf(0xFFE53935));
        } else {
            holder.btnLike.setText("Thích");
            holder.btnLike.setTextColor(ContextCompat.getColor(context, R.color.dlog_navy));
            holder.btnLike.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart_24, 0, 0, 0);
            TextViewCompat.setCompoundDrawableTintList(holder.btnLike, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dlog_muted_blue)));
        }
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
        ImageView ivPostImage;
        TextView tvImagePlaceholder;
        TextView btnLike;
        TextView btnComment;
        TextView btnShare;
        TextView btnSave;
        TextView tvLikesCount;
        TextView tvCaption;
        View captionRow;
        TextView btnCaptionMore;
        TextView btnPreviousPhoto;
        TextView btnNextPhoto;
        int photoIndex;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            llAuthorHeader = itemView.findViewById(R.id.llAuthorHeader);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorUsername = itemView.findViewById(R.id.tvAuthorUsername);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            btnPreviousPhoto = itemView.findViewById(R.id.btnPreviousPhoto);
            btnNextPhoto = itemView.findViewById(R.id.btnNextPhoto);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvImagePlaceholder = itemView.findViewById(R.id.tvImagePlaceholder);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnSave = itemView.findViewById(R.id.btnSave);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            captionRow = itemView.findViewById(R.id.captionRow);
            btnCaptionMore = itemView.findViewById(R.id.btnCaptionMore);
        }
    }
}
