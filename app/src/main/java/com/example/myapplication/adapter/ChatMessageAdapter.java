package com.example.myapplication.adapter;

import android.content.Context;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.model.Post;
import com.example.myapplication.service.impl.ChatService;
import com.example.myapplication.service.impl.PostService;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Collections;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageHolder> {
    private final Context context;
    private final String currentUserId;
    private final List<ChatService.ChatMessage> messages = new ArrayList<>();

    public ChatMessageAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<ChatService.ChatMessage> items) {
        messages.clear();
        if (items != null) messages.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(2, dp(3), 2, dp(3));

        TextView bubble = new TextView(context);
        bubble.setTextSize(15);
        bubble.setPadding(dp(13), dp(9), dp(13), dp(9));
        bubble.setMaxWidth(dp(300));
        row.addView(bubble, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView time = new TextView(context);
        time.setTextSize(12);
        time.setTextColor(ContextCompat.getColor(context, R.color.dlog_muted));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeParams.topMargin = dp(2);
        row.addView(time, timeParams);
        return new MessageHolder(row, bubble, time);
    }

    @Override public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        ChatService.ChatMessage message = messages.get(position);
        boolean mine = currentUserId.equals(message.senderId);
        LinearLayout.LayoutParams bubbleParams = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        bubbleParams.gravity = mine ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(bubbleParams);
        holder.bubble.setBackgroundResource(mine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_theirs);
        holder.bubble.setTextColor(ContextCompat.getColor(context,
                mine ? R.color.dlog_chat_bubble_mine_text : R.color.dlog_chat_bubble_theirs_text));
        if (message.postId != null && !message.postId.trim().isEmpty()) {
            String caption = message.postCaption == null ? "" : message.postCaption.trim();
            if (caption.length() > 120) caption = caption.substring(0, 117) + "…";
            holder.bubble.setText("Bài viết được chia sẻ"
                    + (caption.isEmpty() ? "" : "\n" + caption)
                    + "\nNhấn để xem bài viết");
            holder.bubble.setContentDescription("Bài viết được chia sẻ. Nhấn để xem bài viết");
            holder.bubble.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { openSharedPost(message.postId); }
            });
        } else {
            holder.bubble.setText(message.text);
            holder.bubble.setContentDescription(message.text);
            holder.bubble.setOnClickListener(null);
        }
        LinearLayout.LayoutParams timeParams = (LinearLayout.LayoutParams) holder.time.getLayoutParams();
        timeParams.gravity = mine ? Gravity.END : Gravity.START;
        holder.time.setLayoutParams(timeParams);
        holder.time.setTypeface(ResourcesCompat.getFont(context, R.font.inter_regular));
        holder.time.setText(message.createdAt <= 0 ? "" : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.createdAt)));
    }

    @Override public int getItemCount() { return messages.size(); }

    private void openSharedPost(String postId) {
        PostService.getInstance().getPostDetail(postId, new PostService.Callback<Post>() {
            @Override public void onSuccess(Post post) {
                Dialog dialog = new Dialog(context);
                RecyclerView postList = new RecyclerView(context);
                postList.setLayoutManager(new LinearLayoutManager(context));
                PostAdapter adapter = new PostAdapter(context);
                postList.setAdapter(adapter);
                adapter.setPosts(Collections.singletonList(post));
                dialog.setContentView(postList);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setDimAmount(0.7f);
                    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                }
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                if (dialog.getWindow() != null) {
                    int width = Math.min(context.getResources().getDisplayMetrics().widthPixels - dp(32), dp(540));
                    int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.84f);
                    dialog.getWindow().setLayout(width, height);
                }
            }

            @Override public void onError(Exception error) {
                android.widget.Toast.makeText(context,
                        "Bài viết không còn khả dụng.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int value) { return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f); }

    static class MessageHolder extends RecyclerView.ViewHolder {
        final TextView bubble;
        final TextView time;
        MessageHolder(@NonNull LinearLayout root, TextView bubble, TextView time) {
            super(root);
            this.bubble = bubble;
            this.time = time;
        }
    }
}
