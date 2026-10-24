package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.ChatRowAdapter;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.ChatService;
import com.example.myapplication.service.impl.UserService;
import com.example.myapplication.util.BottomNavHelper;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Inbox and user search entry point for private one-to-one chat. */
public class ChatsActivity extends AppCompatActivity {
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private EditText search;
    private RecyclerView threadsList;
    private RecyclerView peopleList;
    private TextView empty;
    private ProgressBar progress;
    private ChatRowAdapter adapter;
    private ChatService chatService;
    private ListenerRegistration conversationsListener;
    private String currentUserId;
    private String currentQuery = "";
    private List<ChatService.Conversation> conversationCache = new ArrayList<>();
    private final Runnable clearSearch = () -> showThreads();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);
        AuthService auth = AuthService.getInstance();
        auth.init(getApplicationContext());
        currentUserId = auth.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) { finish(); return; }
        chatService = ChatService.getInstance();
        search = findViewById(R.id.etChatSearch);
        threadsList = findViewById(R.id.rvChatThreads);
        peopleList = findViewById(R.id.rvChatPeople);
        empty = findViewById(R.id.tvChatsEmpty);
        progress = findViewById(R.id.progressChats);
        findViewById(R.id.btnBackChats).setOnClickListener(v -> finish());
        adapter = new ChatRowAdapter(this, new ChatRowAdapter.Listener() {
            @Override public void onThread(ChatService.Conversation conversation) {
                openConversation(conversation.id, conversation.otherUserId, conversation.otherName, conversation.otherAvatar);
            }
            @Override public void onUser(User user) { createConversation(user); }
        });
        threadsList.setLayoutManager(new LinearLayoutManager(this));
        peopleList.setLayoutManager(new LinearLayoutManager(this));
        threadsList.setAdapter(adapter);
        peopleList.setAdapter(adapter);
        findViewById(R.id.btnBackChats).setOnClickListener(v -> finish());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onSearchChanged(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.Tab.MENU);
    }

    @Override protected void onStart() {
        super.onStart();
        conversationsListener = chatService.listenConversations(currentUserId, new ChatService.Callback<List<ChatService.Conversation>>() {
            @Override public void onSuccess(List<ChatService.Conversation> items) {
                if (currentQuery.trim().isEmpty()) {
                    conversationCache = items == null ? new ArrayList<>() : new ArrayList<>(items);
                    adapter.setConversations(conversationCache);
                    empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    empty.setText("Chưa có cuộc trò chuyện. Tìm người dùng ở phía trên để bắt đầu.");
                }
            }
            @Override public void onError(Exception error) {
                if (currentQuery.trim().isEmpty()) {
                    empty.setVisibility(View.VISIBLE);
                    empty.setText("Không tải được tin nhắn. Kiểm tra kết nối Firebase.");
                }
            }
        });
    }

    private void onSearchChanged(String raw) {
        currentQuery = raw.trim();
        searchHandler.removeCallbacks(clearSearch);
        if (currentQuery.isEmpty()) { showThreads(); return; }
        threadsList.setVisibility(View.GONE);
        peopleList.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        String query = currentQuery;
        searchHandler.postDelayed(() -> UserService.getInstance().searchUsers(query,
                new UserService.Callback<List<User>>() {
                    @Override public void onSuccess(List<User> users) {
                        if (isFinishing() || !query.equals(currentQuery)) return;
                        progress.setVisibility(View.GONE);
                        List<User> filtered = new ArrayList<>();
                        if (users != null) for (User user : users) {
                            if (user != null && user.getId() != null && !currentUserId.equals(user.getId())) filtered.add(user);
                        }
                        adapter.setUsers(filtered);
                        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                        empty.setText("Không tìm thấy người dùng phù hợp.");
                    }
                    @Override public void onError(Exception error) {
                        if (isFinishing() || !query.equals(currentQuery)) return;
                        progress.setVisibility(View.GONE);
                        empty.setVisibility(View.VISIBLE);
                        empty.setText("Không thể tìm người dùng. Kiểm tra quyền Firestore.");
                    }
                }), 250);
    }

    private void showThreads() {
        currentQuery = "";
        peopleList.setVisibility(View.GONE);
        threadsList.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        adapter.setConversations(conversationCache);
        empty.setVisibility(conversationCache.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText("Chưa có cuộc trò chuyện. Tìm người dùng ở phía trên để bắt đầu.");
    }

    private void createConversation(User user) {
        progress.setVisibility(View.VISIBLE);
        chatService.getOrCreateConversation(currentUserId, user, new ChatService.Callback<String>() {
            @Override public void onSuccess(String conversationId) {
                progress.setVisibility(View.GONE);
                openConversation(conversationId, user.getId(),
                        user.getFullName() == null ? user.getUsername() : user.getFullName(), user.getAvatarUrl());
            }
            @Override public void onError(Exception error) {
                progress.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
                empty.setText("Không mở được cuộc trò chuyện: " + (error.getMessage() == null ? "Lỗi Firebase" : error.getMessage()));
            }
        });
    }

    private void openConversation(String id, String otherId, String name, String avatar) {
        Intent intent = new Intent(this, ChatConversationActivity.class);
        intent.putExtra(ChatConversationActivity.EXTRA_CONVERSATION_ID, id);
        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_USER_ID, otherId);
        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_NAME, name);
        intent.putExtra(ChatConversationActivity.EXTRA_OTHER_AVATAR, avatar);
        startActivity(intent);
    }

    @Override protected void onStop() {
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
        searchHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }
}
