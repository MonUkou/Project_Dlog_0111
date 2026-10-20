package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.service.impl.ChatService;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        time.setTextSize(10);
        time.setTextColor(0xFF7184A1);
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
        holder.bubble.setTextColor(mine ? 0xFFFFFFFF : 0xFF102B50);
        holder.bubble.setText(message.text);
        LinearLayout.LayoutParams timeParams = (LinearLayout.LayoutParams) holder.time.getLayoutParams();
        timeParams.gravity = mine ? Gravity.END : Gravity.START;
        holder.time.setLayoutParams(timeParams);
        holder.time.setTypeface(null, Typeface.NORMAL);
        holder.time.setText(message.createdAt <= 0 ? "" : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.createdAt)));
    }

    @Override public int getItemCount() { return messages.size(); }

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
