package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.PostDetailActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.InteractionService;
import com.example.myapplication.service.impl.PostService;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> posts = new ArrayList<>();
    private final PostService postService;
    private final InteractionService interactionService;
    private final AuthService authService;
    private OnPostChangeListener onPostChangeListener;

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
        String currentUserId = authService.getCurrentUserId();

        // Author username & avatar
        String username = post.getAuthorUsername() != null ? post.getAuthorUsername() : post.getAuthorId();
        holder.tvAuthorUsername.setText(username);

        if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(post.getAuthorAvatarUrl())
                    .circleCrop()
                    .into(holder.ivAuthorAvatar);
        } else {
            holder.ivAuthorAvatar.setImageResource(R.drawable.bg_circle_avatar);
        }

        // Post image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.tvImagePlaceholder.setVisibility(View.GONE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageDrawable(null);
            holder.tvImagePlaceholder.setVisibility(View.VISIBLE);
        }

        // Likes count & caption
        holder.tvLikesCount.setText(post.getLikesCount() + " lượt thích");

        String captionText = username + " " + (post.getCaption() != null ? post.getCaption() : "");
        holder.tvCaption.setText(captionText);

        // Check if current user liked post
        setLikeUI(holder, false);
        interactionService.isLiked(post.getId(), new InteractionService.ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isLiked) {
                setLikeUI(holder, isLiked);
            }

            @Override
            public void onError(String message) {}
        });

        // Check if current user saved post
        postService.isPostSaved(currentUserId, post.getId(), new PostService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isSaved) {
                if (isSaved) {
                    holder.btnSave.setText("Đã lưu");
                    holder.btnSave.setTextColor(0xFF2E7D32);
                } else {
                    holder.btnSave.setText("Lưu");
                    holder.btnSave.setTextColor(0xFF000000);
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        // Click Like button
        holder.btnLike.setOnClickListener(v -> {
            boolean wasLiked = holder.btnLike.getText().toString().equals("Đã thích");
            holder.btnLike.setEnabled(false);
            if (wasLiked) {
                interactionService.unlike(post.getId(), new InteractionService.ServiceCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
                        setLikeUI(holder, false);
                        holder.tvLikesCount.setText(post.getLikesCount() + " lượt thích");
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
                        post.setLikesCount(post.getLikesCount() + 1);
                        setLikeUI(holder, true);
                        holder.tvLikesCount.setText(post.getLikesCount() + " lượt thích");
                        holder.btnLike.setEnabled(true);
                    }

                    @Override
                    public void onError(String message) {
                        holder.btnLike.setEnabled(true);
                        Toast.makeText(context, "Lỗi thao tác thích: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Click Save button (F21: Save, F22: Unsave)
        holder.btnSave.setOnClickListener(v -> {
            boolean wasSaved = holder.btnSave.getText().toString().equals("Đã lưu");
            if (wasSaved) {
                postService.unsavePost(currentUserId, post.getId(), new PostService.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        holder.btnSave.setText("Lưu");
                        holder.btnSave.setTextColor(0xFF000000);
                        Toast.makeText(context, "Đã bỏ lưu bài viết", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Lỗi bỏ lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                postService.savePost(currentUserId, post.getId(), new PostService.Callback<SavedPost>() {
                    @Override
                    public void onSuccess(SavedPost result) {
                        holder.btnSave.setText("Đã lưu");
                        holder.btnSave.setTextColor(0xFF2E7D32);
                        Toast.makeText(context, "Đã lưu bài viết", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Lỗi lưu bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Click Comment button or post image -> open PostDetailActivity
        View.OnClickListener openDetailListener = v -> {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("postId", post.getId());
            context.startActivity(intent);
        };
        holder.btnComment.setOnClickListener(openDetailListener);
        holder.ivPostImage.setOnClickListener(openDetailListener);
        holder.btnShare.setOnClickListener(v -> sharePost(post));

        // Click author header -> open ProfileActivity
        View.OnClickListener openProfileListener = v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", post.getAuthorId());
            context.startActivity(intent);
        };
        holder.llAuthorHeader.setOnClickListener(openProfileListener);

        // Long press post -> Option dialog (Save image to device F24, Edit/Delete if author)
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                showPostOptionsDialog(post, currentUserId, pos);
            }
            return true;
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
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
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

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newCaption = input.getText().toString().trim();
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
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showDeletePostConfirmDialog(Post post, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bài viết")
                .setMessage("Bạn có chắc chắn muốn xóa bài viết này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
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
        ImageView ivPostImage;
        TextView tvImagePlaceholder;
        TextView btnLike;
        TextView btnComment;
        TextView btnShare;
        TextView btnSave;
        TextView tvLikesCount;
        TextView tvCaption;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            llAuthorHeader = itemView.findViewById(R.id.llAuthorHeader);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorUsername = itemView.findViewById(R.id.tvAuthorUsername);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvImagePlaceholder = itemView.findViewById(R.id.tvImagePlaceholder);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnSave = itemView.findViewById(R.id.btnSave);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            tvCaption = itemView.findViewById(R.id.tvCaption);
        }
    }
}
