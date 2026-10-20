package com.example.myapplication.service.impl;

import com.example.myapplication.model.Notification;
import com.example.myapplication.service.INotificationService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationService implements INotificationService {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static NotificationService instance;
    private final FirebaseFirestore db;

    public NotificationService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    @Override
    public Notification createNotification(String receiverId, String senderId, String type, String postId) {
        if (receiverId == null || receiverId.equals(senderId)) return null;

        String id = db.collection("notifications").document().getId();
        Notification notification = new Notification(id, receiverId, senderId, senderId, type, postId);

        db.collection("notifications").document(id).set(notification.toMap());
        return notification;
    }

    public void createNotificationAsync(String receiverId, String senderId, String senderUsername, String type, String postId) {
        if (receiverId == null || receiverId.equals(senderId)) return;

        String id = db.collection("notifications").document().getId();
        Notification notification = new Notification(id, receiverId, senderId, senderUsername, type, postId);

        db.collection("notifications").document(id).set(notification.toMap());
    }

    @Override
    public List<Notification> getNotifications(String userId) {
        return new ArrayList<>();
    }

    public void getNotificationsAsync(String userId, Callback<List<Notification>> callback) {
        if (userId == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (doc.getData() != null) {
                            Notification n = Notification.fromMap(doc.getData());
                            if (n != null) list.add(n);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    db.collection("notifications")
                            .whereEqualTo("receiverId", userId)
                            .get()
                            .addOnSuccessListener(querySnapshot2 -> {
                                List<Notification> list = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot2.getDocuments()) {
                                    if (doc.getData() != null) {
                                        Notification n = Notification.fromMap(doc.getData());
                                        if (n != null) list.add(n);
                                    }
                                }
                                list.sort((n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt()));
                                callback.onSuccess(list);
                            })
                            .addOnFailureListener(callback::onError);
                });
    }

    @Override
    public void markAsRead(String notificationId) {
        if (notificationId != null) {
            db.collection("notifications").document(notificationId).update("read", true);
        }
    }

    public void markAsReadAsync(String notificationId, String userId, Callback<Void> callback) {
        markAsRead(notificationId);
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public Integer getUnreadCount(String userId) {
        return 0;
    }

    public void countUnreadAsync(String userId, Callback<Integer> callback) {
        getUnreadCountAsync(userId, callback);
    }

    public void getUnreadCountAsync(String userId, Callback<Integer> callback) {
        if (userId == null) {
            callback.onSuccess(0);
            return;
        }

        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(qs -> callback.onSuccess(qs.size()))
                .addOnFailureListener(e -> callback.onSuccess(0));
    }
}
