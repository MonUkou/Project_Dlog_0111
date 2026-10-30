package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostGridAdapter extends RecyclerView.Adapter<PostGridAdapter.GridViewHolder> {

    private final Context context;
    private final List<Post> posts = new ArrayList<>();
    public interface OnPostClickListener { void onPostClick(Post post); }
    private OnPostClickListener onPostClickListener;

    public PostGridAdapter(Context context) {
        this.context = context;
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }

    public void setPosts(List<Post> newPosts) {
        this.posts.clear();
        if (newPosts != null) {
            this.posts.addAll(newPosts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Post post = posts.get(position);

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.tvGridPlaceholder.setVisibility(View.GONE);
            Glide.with(context)
                    .load(post.getImageUrl())
                    .centerCrop()
                    .into(holder.ivGridImage);
        } else {
            holder.ivGridImage.setImageDrawable(null);
            holder.tvGridPlaceholder.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onPostClickListener != null) onPostClickListener.onPostClick(post);
                else com.example.myapplication.PhotoViewerActivity.open(context, post, 0);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGridImage;
        TextView tvGridPlaceholder;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGridImage = itemView.findViewById(R.id.ivGridImage);
            tvGridPlaceholder = itemView.findViewById(R.id.tvGridPlaceholder);
        }
    }
}
