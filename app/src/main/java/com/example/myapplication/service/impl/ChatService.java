package com.example.myapplication.service.impl;

import com.example.myapplication.model.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firestore-backed one-to-one conversations and realtime messages. */
public final class ChatService {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    public static final class Conversation {
        public final String id;
        public final String otherUserId;
        public final String otherName;
        public final String otherAvatar;
        public final String lastMessage;
        public final long updatedAt;

        Conversation(String id, String otherUserId, String otherName,
                     String otherAvatar, String lastMessage, long updatedAt) {
            this.id = id;
            this.otherUserId = otherUserId;
            this.otherName = otherName;
            this.otherAvatar = otherAvatar;
            this.lastMessage = lastMessage;
            this.updatedAt = updatedAt;
        }
    }

    public static final class ChatMessage {
        public final String id;
        public final String senderId;
        public final String text;
        public final long createdAt;
        public final String postId;
        public final String postCaption;

        ChatMessage(String id, String senderId, String text, long createdAt,
                    String postId, String postCaption) {
            this.id = id;
            this.senderId = senderId;
            this.text = text;
            this.createdAt = createdAt;
            this.postId = postId;
            this.postCaption = postCaption;
        }
    }

    private static ChatService instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized ChatService getInstance() {
        if (instance == null) instance = new ChatService();
        return instance;
    }

    public void getOrCreateConversation(String currentUserId, User other, Callback<String> callback) {
        if (currentUserId == null || currentUserId.isEmpty() || other == null
                || other.getId() == null || other.getId().isEmpty()
                || currentUserId.equals(other.getId())) {
            callback.onError(new IllegalArgumentException("Tài khoản trò chuyện không hợp lệ."));
            return;
        }
        final String otherId = other.getId();
        final String conversationId = currentUserId.compareTo(otherId) < 0
                ? currentUserId + "__" + otherId : otherId + "__" + currentUserId;
        db.collection("chats").document(conversationId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot snapshot) {
                    if (snapshot.exists()) {
                        callback.onSuccess(conversationId);
                        return;
                    }
                    db.collection("users").document(currentUserId).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override public void onSuccess(DocumentSnapshot current) {
                                Map<String, Object> names = new HashMap<>();
                                Map<String, Object> avatars = new HashMap<>();
                                names.put(currentUserId, firstNonEmpty(current.getString("fullName"),
                                        current.getString("username"), "Người dùng DLog"));
                                names.put(otherId, firstNonEmpty(other.getFullName(), other.getUsername(), "Người dùng DLog"));
                                avatars.put(currentUserId, current.getString("avatarUrl") == null ? "" : current.getString("avatarUrl"));
                                avatars.put(otherId, other.getAvatarUrl() == null ? "" : other.getAvatarUrl());
                                Map<String, Object> data = new HashMap<>();
                                data.put("participants", java.util.Arrays.asList(currentUserId, otherId));
                                data.put("participantNames", names);
                                data.put("participantAvatars", avatars);
                                data.put("lastMessage", "");
                                data.put("lastSenderId", "");
                                data.put("updatedAt", FieldValue.serverTimestamp());
                                db.collection("chats").document(conversationId)
                                        .set(data, SetOptions.merge())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override public void onSuccess(Void unused) {
                                                callback.onSuccess(conversationId);
                                            }
                                        })
                                        .addOnFailureListener(callback::onError);
                                }
                            })
                            .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenConversations(String currentUserId, Callback<List<Conversation>> callback) {
        return db.collection("chats").whereArrayContains("participants", currentUserId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException error) {
                    if (error != null) { callback.onError(error); return; }
                    List<Conversation> items = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            List<String> participants = (List<String>) doc.get("participants");
                            if (participants == null) continue;
                            String otherId = null;
                            for (String participant : participants) {
                                if (participant != null && !participant.equals(currentUserId)) otherId = participant;
                            }
                            if (otherId == null) continue;
                            Map<String, Object> names = (Map<String, Object>) doc.get("participantNames");
                            Map<String, Object> avatars = (Map<String, Object>) doc.get("participantAvatars");
                            String name = names == null ? null : stringValue(names.get(otherId));
                            String avatar = avatars == null ? null : stringValue(avatars.get(otherId));
                            items.add(new Conversation(doc.getId(), otherId,
                                    firstNonEmpty(name, otherId, "Người dùng DLog"),
                                    avatar == null ? "" : avatar,
                                    stringValue(doc.get("lastMessage")), timestampValue(doc.get("updatedAt"))));
                        }
                    }
                    Collections.sort(items, new Comparator<Conversation>() {
                        @Override public int compare(Conversation left, Conversation right) {
                            return Long.compare(right.updatedAt, left.updatedAt);
                        }
                    });
                    callback.onSuccess(items);
                    }
                });
    }

    public ListenerRegistration listenMessages(String conversationId, Callback<List<ChatMessage>> callback) {
        return db.collection("chats").document(conversationId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(300)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException error) {
                    if (error != null) { callback.onError(error); return; }
                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            messages.add(new ChatMessage(doc.getId(), stringValue(doc.get("senderId")),
                                    stringValue(doc.get("text")), timestampValue(doc.get("createdAt")),
                                    stringValue(doc.get("postId")), stringValue(doc.get("postCaption"))));
                        }
                    }
                    callback.onSuccess(messages);
                    }
                });
    }

    public void sendMessage(String conversationId, String senderId, String senderName,
                            String text, Callback<Void> callback) {
        String cleanText = text == null ? "" : text.trim();
        if (conversationId == null || senderId == null || cleanText.isEmpty()) {
            callback.onError(new IllegalArgumentException("Không thể gửi tin nhắn trống."));
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", senderId);
        message.put("senderName", firstNonEmpty(senderName, null, "Người dùng DLog"));
        message.put("text", cleanText);
        message.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> threadUpdate = new HashMap<>();
        threadUpdate.put("lastMessage", cleanText);
        threadUpdate.put("lastSenderId", senderId);
        threadUpdate.put("updatedAt", FieldValue.serverTimestamp());

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("chats").document(conversationId)
                .collection("messages").document(), message);
        batch.set(db.collection("chats").document(conversationId), threadUpdate, SetOptions.merge());
        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override public void onSuccess(Void unused) { callback.onSuccess(null); }
                })
                .addOnFailureListener(callback::onError);
    }

    /** Sends a reference to a DLog post so the receiver can reopen the full post card. */
    public void sendPost(String conversationId, String senderId, String senderName,
                         String postId, String postCaption, Callback<Void> callback) {
        if (conversationId == null || senderId == null || postId == null || postId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Bài viết chia sẻ không hợp lệ."));
            return;
        }
        String caption = postCaption == null ? "" : postCaption.trim();
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", senderId);
        message.put("senderName", firstNonEmpty(senderName, null, "Người dùng DLog"));
        message.put("text", "Đã chia sẻ một bài viết");
        message.put("type", "post");
        message.put("postId", postId);
        message.put("postCaption", caption);
        message.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> threadUpdate = new HashMap<>();
        threadUpdate.put("lastMessage", "Đã chia sẻ một bài viết");
        threadUpdate.put("lastSenderId", senderId);
        threadUpdate.put("updatedAt", FieldValue.serverTimestamp());

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("chats").document(conversationId)
                .collection("messages").document(), message);
        batch.set(db.collection("chats").document(conversationId), threadUpdate, SetOptions.merge());
        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override public void onSuccess(Void unused) { callback.onSuccess(null); }
                })
                .addOnFailureListener(callback::onError);
    }

    private static String firstNonEmpty(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) return first;
        if (second != null && !second.trim().isEmpty()) return second;
        return fallback;
    }

    private static String stringValue(Object value) { return value instanceof String ? (String) value : ""; }

    private static long timestampValue(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
