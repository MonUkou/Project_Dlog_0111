package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.ProfileActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

/** Displays followed accounts in the horizontal people strip. */
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
        if (items != null) {
            for (User user : items) {
                if (user != null && user.getId() != null
                        && !user.getId().equals(currentUserId)) users.add(user);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setLayoutParams(new RecyclerView.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
        item.setPadding(dp(3), dp(2), dp(3), 0);

        FrameLayout avatarFrame = new FrameLayout(context);
        avatarFrame.setBackgroundResource(R.drawable.bg_story_ring);
        avatarFrame.setPadding(dp(3), dp(3), dp(3), dp(3));
        item.addView(avatarFrame, new LinearLayout.LayoutParams(dp(58), dp(58)));

        ImageView avatar = new ImageView(context);
        avatar.setBackgroundResource(R.drawable.bg_story_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarFrame.addView(avatar, new FrameLayout.LayoutParams(-1, -1));

        TextView name = new TextView(context);
        name.setGravity(Gravity.CENTER);
        name.setTextColor(ContextCompat.getColor(context, R.color.dlog_ink));
        name.setTextSize(12);
        name.setTypeface(ResourcesCompat.getFont(context, R.font.inter_medium));
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(3);
        item.addView(name, nameParams);
        return new UserViewHolder(item, avatar, name);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        final User user = users.get(position);
        String name = user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName() : user.getUsername();
        holder.name.setText(name == null || name.isEmpty() ? "Người dùng" : name);
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.bg_circle_avatar);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
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
        UserViewHolder(View item, ImageView avatar, TextView name) {
            super(item);
            this.avatar = avatar;
            this.name = name;
        }
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
