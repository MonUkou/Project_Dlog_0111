package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
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
import com.example.myapplication.PostDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifyViewHolder> {

    private final Context context;
    private final List<Notification> notifications = new ArrayList<>();

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public void setNotifications(List<Notification> newNotifications) {
        this.notifications.clear();
        if (newNotifications != null) {
            this.notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotifyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifyViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        String sender = notification.getSenderUsername() != null ? notification.getSenderUsername() : notification.getSenderId();
        String actionText = "";
        if ("LIKE".equalsIgnoreCase(notification.getType())) {
            actionText = " đã thích bài viết của bạn";
        } else if ("COMMENT".equalsIgnoreCase(notification.getType())) {
            actionText = " đã bình luận bài viết của bạn";
        } else if ("FOLLOW".equalsIgnoreCase(notification.getType())) {
            actionText = " đã bắt đầu theo dõi bạn";
        }

        String fullText = sender + actionText;
        SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);
        if (sender != null && !sender.isEmpty()) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, sender.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.tvNotifyText.setText(ssb);

        if (notification.isRead()) {
            holder.vUnreadDot.setVisibility(View.GONE);
        } else {
            holder.vUnreadDot.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (notification.getPostId() != null && !notification.getPostId().isEmpty()) {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", notification.getPostId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotifyViewHolder extends RecyclerView.ViewHolder {
        ImageView ivNotifyAvatar;
        TextView tvNotifyText;
        View vUnreadDot;

        public NotifyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivNotifyAvatar = itemView.findViewById(R.id.ivNotifyAvatar);
            tvNotifyText = itemView.findViewById(R.id.tvNotifyText);
            vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
