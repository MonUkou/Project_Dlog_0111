package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.ChatMessageAdapter;
import com.example.myapplication.model.User;
import com.example.myapplication.service.impl.AuthService;
import com.example.myapplication.service.impl.ChatService;
import com.example.myapplication.service.impl.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ChatConversationActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    public static final String EXTRA_OTHER_USER_ID = "otherUserId";
    public static final String EXTRA_OTHER_NAME = "otherUserName";
    public static final String EXTRA_OTHER_AVATAR = "otherUserAvatar";

    private String conversationId;
    private String currentUserId;
    private String senderName = "Người dùng DLog";
    private EditText input;
    private ImageView send;
    private RecyclerView list;
    private TextView empty;
    private ChatMessageAdapter adapter;
    private ChatService chatService;
    private ListenerRegistration messagesListener;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_conversation);
        AuthService auth = AuthService.getInstance();
        auth.init(getApplicationContext());
        currentUserId = auth.getCurrentUserId();
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        if (currentUserId == null || conversationId == null) { finish(); return; }
        chatService = ChatService.getInstance();

        String otherName = getIntent().getStringExtra(EXTRA_OTHER_NAME);
        ((TextView) findViewById(R.id.tvConversationName)).setText(
                otherName == null || otherName.trim().isEmpty() ? "Tin nhắn" : otherName);
        String avatar = getIntent().getStringExtra(EXTRA_OTHER_AVATAR);
        ImageView avatarView = findViewById(R.id.ivConversationAvatar);
        if (avatar != null && !avatar.isEmpty()) Glide.with(this).load(avatar).placeholder(R.drawable.bg_circle_avatar).error(R.drawable.bg_circle_avatar).circleCrop().placeholder(R.drawable.bg_circle_avatar).into(avatarView);
        else avatarView.setImageResource(R.drawable.bg_circle_avatar);

        findViewById(R.id.btnBackConversation).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        input = findViewById(R.id.etChatMessage);
        send = findViewById(R.id.btnSendChatMessage);
        list = findViewById(R.id.rvChatMessages);
        empty = findViewById(R.id.tvMessagesEmpty);
        adapter = new ChatMessageAdapter(this, currentUserId);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        list.setLayoutManager(manager);
        list.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { sendMessage(); }
        });

        UserService.getInstance().getUserProfile(currentUserId, new UserService.Callback<User>() {
            @Override public void onSuccess(User user) {
                if (user != null) senderName = user.getFullName() == null ? user.getUsername() : user.getFullName();
            }
            @Override public void onError(Exception error) { }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (conversationId == null) return;
        messagesListener = chatService.listenMessages(conversationId, new ChatService.Callback<List<ChatService.ChatMessage>>() {
            @Override public void onSuccess(List<ChatService.ChatMessage> messages) {
                adapter.setMessages(messages);
                boolean isEmpty = messages == null || messages.isEmpty();
                empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (!isEmpty) list.scrollToPosition(messages.size() - 1);
            }
            @Override public void onError(Exception error) {
                empty.setVisibility(View.VISIBLE);
                empty.setText("Không tải được tin nhắn. Kiểm tra quyền Firestore.");
            }
        });
    }

    private void sendMessage() {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        send.setEnabled(false);
        chatService.sendMessage(conversationId, currentUserId, senderName, text, new ChatService.Callback<Void>() {
            @Override public void onSuccess(Void unused) {
                input.setText("");
                send.setEnabled(true);
            }
            @Override public void onError(Exception error) {
                send.setEnabled(true);
                Toast.makeText(ChatConversationActivity.this,
                        "Không gửi được tin nhắn: " + (error.getMessage() == null ? "Lỗi Firebase" : error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override protected void onStop() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        super.onStop();
    }
}
