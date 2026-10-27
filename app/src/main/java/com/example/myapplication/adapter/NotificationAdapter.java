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
import com.example.myapplication.HomeActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Notification;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifyViewHolder> {

    private final Context context;
    private final List<Notification> notifications = new ArrayList<>();
    private final Map<String, User> senderCache = new HashMap<>();
    private final Set<String> loadingSenderIds = new HashSet<>();

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
        User senderProfile = senderCache.get(notification.getSenderId());
        bindSender(holder, notification, senderProfile);
        loadSenderProfile(notification.getSenderId());

        if (notification.isRead()) {
            holder.vUnreadDot.setVisibility(View.GONE);
        } else {
            holder.vUnreadDot.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            if (notification.getPostId() != null && !notification.getPostId().isEmpty()) {
                Intent intent = new Intent(context, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
            }
        });
    }

    private void bindSender(NotifyViewHolder holder, Notification notification, User profile) {
        String sender = displayName(profile, notification);
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

        String avatarUrl = profile == null ? null : profile.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            Glide.with(context).clear(holder.ivNotifyAvatar);
            holder.ivNotifyAvatar.setImageResource(R.drawable.bg_circle_avatar);
        } else {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.bg_circle_avatar)
                    .error(R.drawable.bg_circle_avatar)
                    .circleCrop()
                    .into(holder.ivNotifyAvatar);
        }
    }

    private String displayName(User profile, Notification notification) {
        if (profile != null) {
            if (profile.getFullName() != null && !profile.getFullName().trim().isEmpty()
                    && !profile.getFullName().equals(notification.getSenderId())) {
                return profile.getFullName().trim();
            }
            if (profile.getUsername() != null && !profile.getUsername().trim().isEmpty()
                    && !profile.getUsername().equals(notification.getSenderId())) {
                return profile.getUsername().trim();
            }
        }
        String storedName = notification.getSenderUsername();
        if (storedName != null && !storedName.trim().isEmpty()
                && !storedName.equals(notification.getSenderId())) {
            return storedName.trim();
        }
        return "Người dùng DLog";
    }

    private void loadSenderProfile(final String senderId) {
        if (senderId == null || senderId.trim().isEmpty()
                || senderCache.containsKey(senderId) || loadingSenderIds.contains(senderId)) return;
        loadingSenderIds.add(senderId);
        UserService.getInstance().getUserProfile(senderId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                loadingSenderIds.remove(senderId);
                if (user != null) senderCache.put(senderId, user);
                refreshSenderRows(senderId);
            }

            @Override
            public void onError(Exception error) {
                loadingSenderIds.remove(senderId);
            }
        });
    }

    private void refreshSenderRows(String senderId) {
        for (int index = 0; index < notifications.size(); index++) {
            if (senderId.equals(notifications.get(index).getSenderId())) notifyItemChanged(index);
        }
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
