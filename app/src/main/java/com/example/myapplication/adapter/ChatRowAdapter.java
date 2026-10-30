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
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.ChatService;

import java.util.ArrayList;
import java.util.List;

public class ChatRowAdapter extends RecyclerView.Adapter<ChatRowAdapter.RowHolder> {
    public interface Listener {
        void onThread(ChatService.Conversation conversation);
        void onUser(User user);
    }

    private final Context context;
    private final Listener listener;
    private final List<ChatService.Conversation> conversations = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private boolean showingUsers;

    public ChatRowAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setConversations(List<ChatService.Conversation> items) {
        showingUsers = false;
        conversations.clear();
        if (items != null) conversations.addAll(items);
        notifyDataSetChanged();
    }

    public void setUsers(List<User> items) {
        showingUsers = true;
        users.clear();
        if (items != null) users.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(LayoutInflater.from(context).inflate(R.layout.item_chat_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        if (showingUsers) {
            bindUser(holder, users.get(position));
        } else {
            bindConversation(holder, conversations.get(position));
        }
    }

    private void bindUser(@NonNull RowHolder holder, @NonNull User user) {
        holder.name.setText(value(user.getFullName(), value(user.getUsername(), "Người dùng DLog")));
        holder.subtitle.setText("@" + value(user.getUsername(), "dlog_user") + " · Nhấn để nhắn tin");
        bindAvatar(holder, user.getAvatarUrl());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= users.size()) return;
                listener.onUser(users.get(adapterPosition));
            }
        });
    }

    private void bindConversation(@NonNull RowHolder holder,
                                  @NonNull ChatService.Conversation conversation) {
        holder.name.setText(conversation.otherName);
        holder.subtitle.setText(conversation.lastMessage.isEmpty()
                ? "Bắt đầu trò chuyện" : conversation.lastMessage);
        bindAvatar(holder, conversation.otherAvatar);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION
                        || adapterPosition >= conversations.size()) return;
                listener.onThread(conversations.get(adapterPosition));
            }
        });
    }

    private void bindAvatar(@NonNull RowHolder holder, String avatar) {
        if (avatar != null && !avatar.trim().isEmpty()) {
            Glide.with(context).load(avatar).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().placeholder(R.drawable.bg_circle_avatar).into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.bg_circle_avatar);
        }
    }

    @Override public int getItemCount() { return showingUsers ? users.size() : conversations.size(); }

    private static String value(String v, String fallback) { return v == null || v.trim().isEmpty() ? fallback : v; }

    static class RowHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name;
        final TextView subtitle;
        RowHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ivChatRowAvatar);
            name = itemView.findViewById(R.id.tvChatRowName);
            subtitle = itemView.findViewById(R.id.tvChatRowSubtitle);
        }
    }
}
