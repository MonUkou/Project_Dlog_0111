package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.CreatePostActivity;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

/** Displays real DLog accounts in the horizontal people strip. */
public class UserStripAdapter extends RecyclerView.Adapter<UserStripAdapter.UserViewHolder> {
    private final Context context;
    private final String currentUserId;
    private final List<User> users = new ArrayList<>();

    public UserStripAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setUsers(List<User> items) {
        users.clear();
        if (items != null) users.addAll(items);
        if (currentUserId != null) {
            users.sort((left, right) -> Boolean.compare(
                    !currentUserId.equals(left.getId()), !currentUserId.equals(right.getId())));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setLayoutParams(new RecyclerView.LayoutParams(dp(68), ViewGroup.LayoutParams.WRAP_CONTENT));
        item.setPadding(dp(3), dp(2), dp(3), 0);

        FrameLayout avatarFrame = new FrameLayout(context);
        avatarFrame.setBackgroundResource(R.drawable.bg_story_ring);
        avatarFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
        item.addView(avatarFrame, new LinearLayout.LayoutParams(dp(58), dp(58)));

        ImageView avatar = new ImageView(context);
        avatar.setBackgroundResource(R.drawable.bg_story_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarFrame.addView(avatar, new FrameLayout.LayoutParams(-1, -1));

        TextView addBadge = new TextView(context);
        addBadge.setText("+");
        addBadge.setTextColor(Color.WHITE);
        addBadge.setTextSize(14);
        addBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        addBadge.setGravity(Gravity.CENTER);
        addBadge.setBackgroundResource(R.drawable.bg_story_add);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.BOTTOM | Gravity.END);
        avatarFrame.addView(addBadge, badgeParams);

        TextView name = new TextView(context);
        name.setGravity(Gravity.CENTER);
        name.setTextColor(Color.rgb(23, 43, 77));
        name.setTextSize(10);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(3);
        item.addView(name, nameParams);
        return new UserViewHolder(item, avatar, name, addBadge);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        boolean isCurrentUser = currentUserId != null && currentUserId.equals(user.getId());
        String name = user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName() : user.getUsername();
        holder.name.setText(name == null || name.isEmpty() ? "Người dùng" : name);
        holder.addBadge.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context).load(user.getAvatarUrl()).circleCrop().into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.bg_circle_avatar);
        }
        holder.itemView.setOnClickListener(v -> {
            if (isCurrentUser) {
                context.startActivity(new Intent(context, CreatePostActivity.class));
            } else {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("userId", user.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name;
        final TextView addBadge;
        UserViewHolder(View item, ImageView avatar, TextView name, TextView addBadge) {
            super(item);
            this.avatar = avatar;
            this.name = name;
            this.addBadge = addBadge;
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
