package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Comment;
import com.example.myapplication.util.UiMotion;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> comments = new ArrayList<>();
    private int lastAnimatedPosition = -1;

    public CommentAdapter(Context context) {
        this.context = context;
    }

    public void setComments(List<Comment> newComments) {
        this.comments.clear();
        lastAnimatedPosition = -1;
        if (newComments != null) {
            this.comments.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            this.comments.add(comment);
            notifyItemInserted(this.comments.size() - 1);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        String username = comment.getUsername() != null && !comment.getUsername().isEmpty()
                ? comment.getUsername() : comment.getAuthorId();
        String text = username + " " + (comment.getContent() != null ? comment.getContent() : "");

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        if (username != null && !username.isEmpty()) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvCommentContent.setText(ssb);

        if (comment.getAvatarUrl() != null && !comment.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(comment.getAvatarUrl())
                    .placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop()
                    .into(holder.ivCommentAvatar);
        } else {
            holder.ivCommentAvatar.setImageResource(R.drawable.bg_circle_avatar);
        }
        int adapterPosition = holder.getBindingAdapterPosition();
        if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition > lastAnimatedPosition) {
            UiMotion.reveal(holder.itemView, adapterPosition);
            lastAnimatedPosition = adapterPosition;
        }
    }

    @Override
    public void onViewRecycled(@NonNull CommentViewHolder holder) {
        holder.itemView.animate().cancel();
        holder.itemView.setAlpha(1f);
        holder.itemView.setTranslationY(0f);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentAvatar;
        TextView tvCommentContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCommentAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
        }
    }
}
