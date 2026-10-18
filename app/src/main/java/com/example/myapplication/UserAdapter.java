package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;
    private final OnFollowClickListener followListener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnFollowClickListener {
        void onFollowClick(User user, Button btnFollow);
    }

    public UserAdapter(OnUserClickListener listener, OnFollowClickListener followListener) {
        this.listener = listener;
        this.followListener = followListener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvItemDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
        holder.tvItemUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : ""));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });

        if (followListener != null) {
            holder.btnFollowAction.setVisibility(View.VISIBLE);
            holder.btnFollowAction.setOnClickListener(v -> followListener.onFollowClick(user, holder.btnFollowAction));
        } else {
            holder.btnFollowAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvItemDisplayName;
        TextView tvItemUsername;
        Button btnFollowAction;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvItemDisplayName = itemView.findViewById(R.id.tvItemDisplayName);
            tvItemUsername = itemView.findViewById(R.id.tvItemUsername);
            btnFollowAction = itemView.findViewById(R.id.btnFollowAction);
        }
    }
}
