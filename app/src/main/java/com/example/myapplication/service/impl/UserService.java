package com.example.myapplication.service.impl;

import com.example.myapplication.model.Follow;
import com.example.myapplication.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static UserService instance;
    private final FirebaseFirestore db;

    public UserService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void getUserProfile(String userId, Callback<User> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("UserId không hợp lệ."));
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        User user = User.fromMap(documentSnapshot.getData());
                        callback.onSuccess(user);
                    } else {
                        // Create standard fallback user for display
                        User user = new User(userId, userId, userId, userId + "@example.com", "", "Giới thiệu ngắn về bản thân.");
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    User user = new User(userId, userId, userId, userId + "@example.com", "", "Giới thiệu ngắn về bản thân.");
                    callback.onSuccess(user);
                });
    }

    public void updateUserProfile(User user, Callback<User> callback) {
        if (user == null || user.getId() == null) {
            callback.onError(new IllegalArgumentException("User không hợp lệ."));
            return;
        }

        user.touch();
        Map<String, Object> profileFields = new HashMap<>();
        profileFields.put("username", user.getUsername());
        profileFields.put("fullName", user.getFullName());
        profileFields.put("bio", user.getBio());
        profileFields.put("avatarUrl", user.getAvatarUrl());
        profileFields.put("updatedAt", user.getUpdatedAt());
        db.collection("users").document(user.getId())
                // Only profile fields are written; role and social counters stay intact.
                .set(profileFields, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                .addOnFailureListener(callback::onError);
    }

    public void followUser(String followerId, String followingId, Callback<Follow> callback) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = followerId + "_" + followingId;
        Follow follow = new Follow(followerId, followingId);

        db.collection("follows").document(docId)
                .set(follow.toMap())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(followerId).update("followingCount", FieldValue.increment(1));
                    db.collection("users").document(followingId).update("followersCount", FieldValue.increment(1));
                    callback.onSuccess(follow);
                })
                .addOnFailureListener(callback::onError);
    }

    public void unfollowUser(String followerId, String followingId, Callback<Void> callback) {
        if (followerId == null || followingId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = followerId + "_" + followingId;
        db.collection("follows").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(followerId).update("followingCount", FieldValue.increment(-1));
                    db.collection("users").document(followingId).update("followersCount", FieldValue.increment(-1));
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    public void isFollowing(String followerId, String followingId, Callback<Boolean> callback) {
        if (followerId == null || followingId == null) {
            callback.onSuccess(false);
            return;
        }

        String docId = followerId + "_" + followingId;
        db.collection("follows").document(docId).get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    public void searchUsers(String queryText, Callback<List<User>> callback) {
        if (queryText == null || queryText.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    String searchLower = queryText.trim().toLowerCase();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (doc.getData() != null) {
                            User u = User.fromMap(doc.getData());
                            if (u != null) {
                                boolean nameMatch = u.getUsername() != null && u.getUsername().toLowerCase().contains(searchLower);
                                boolean fullMatch = u.getFullName() != null && u.getFullName().toLowerCase().contains(searchLower);
                                if (nameMatch || fullMatch) {
                                    users.add(u);
                                }
                            }
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onError);
    }
}
