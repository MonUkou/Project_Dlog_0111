package com.example.myapplication.service.impl;

import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.model.Follow;
import com.example.myapplication.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class UserService {
    private static final String TAG = "UserService";
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;
    private final CollectionReference followsRef;
    private final FirebaseStorage storage;

    public UserService() {
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
        this.followsRef = db.collection("follows");
        this.storage = FirebaseStorage.getInstance();
    }

    public UserService(FirebaseFirestore db, FirebaseStorage storage) {
        this.db = db;
        this.usersRef = db.collection("users");
        this.followsRef = db.collection("follows");
        this.storage = storage;
    }

    // F06: Xem hồ sơ
    public User getUserProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        try {
            DocumentSnapshot doc = awaitTask(usersRef.document(userId).get());
            if (doc.exists()) {
                return User.fromMap(doc.getData(), doc.getId());
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "getUserProfile error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get user profile: " + e.getMessage(), e);
        }
    }

    // F07: Cập nhật hồ sơ
    public void updateProfile(String userId, String displayName, String bio, String avatarUrl) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        try {
            User user = getUserProfile(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            user.updateProfile(displayName, bio, avatarUrl);
            awaitTask(usersRef.document(userId).set(user.toMap()));
        } catch (Exception e) {
            Log.e(TAG, "updateProfile error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to update profile: " + e.getMessage(), e);
        }
    }

    // F08: Đổi ảnh đại diện
    public String uploadAvatar(String userId, Uri imageUri) {
        if (userId == null || userId.trim().isEmpty() || imageUri == null) {
            throw new IllegalArgumentException("User ID and image URI cannot be null");
        }
        try {
            String fileName = "avatars/" + userId + "_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);
            awaitTask(ref.putFile(imageUri));
            Uri downloadUri = awaitTask(ref.getDownloadUrl());
            String avatarUrl = downloadUri.toString();

            User user = getUserProfile(userId);
            if (user != null) {
                user.updateProfile(user.getDisplayName(), user.getBio(), avatarUrl);
                awaitTask(usersRef.document(userId).set(user.toMap()));
            }
            return avatarUrl;
        } catch (Exception e) {
            Log.e(TAG, "uploadAvatar error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage(), e);
        }
    }

    // F09: Theo dõi người dùng
    public void followUser(String followerId, String followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            throw new IllegalArgumentException("Invalid follower or following ID (cannot follow self)");
        }
        try {
            if (isFollowing(followerId, followingId)) {
                return;
            }

            String followId = followerId + "_" + followingId;
            Follow follow = new Follow(followId, followerId, followingId);
            if (!follow.validate()) {
                throw new IllegalArgumentException("Invalid follow relation");
            }

            awaitTask(followsRef.document(followId).set(follow.toMap()));

            awaitTask(usersRef.document(followerId).update("followingCount", FieldValue.increment(1)));
            awaitTask(usersRef.document(followingId).update("followerCount", FieldValue.increment(1)));

        } catch (Exception e) {
            Log.e(TAG, "followUser error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to follow user: " + e.getMessage(), e);
        }
    }

    // F10: Bỏ theo dõi
    public void unfollowUser(String followerId, String followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            throw new IllegalArgumentException("Invalid follower or following ID");
        }
        try {
            String followId = followerId + "_" + followingId;
            DocumentSnapshot doc = awaitTask(followsRef.document(followId).get());
            if (doc.exists()) {
                awaitTask(followsRef.document(followId).delete());

                awaitTask(usersRef.document(followerId).update("followingCount", FieldValue.increment(-1)));
                awaitTask(usersRef.document(followingId).update("followerCount", FieldValue.increment(-1)));
            }
        } catch (Exception e) {
            Log.e(TAG, "unfollowUser error: " + e.getMessage(), e);
            throw new RuntimeException("Failed to unfollow user: " + e.getMessage(), e);
        }
    }

    // F11: Kiểm tra đang theo dõi
    public boolean isFollowing(String followerId, String followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        try {
            String followId = followerId + "_" + followingId;
            DocumentSnapshot doc = awaitTask(followsRef.document(followId).get());
            return doc.exists();
        } catch (Exception e) {
            Log.e(TAG, "isFollowing error: " + e.getMessage(), e);
            return false;
        }
    }

    // F12: Danh sách người theo dõi
    public List<User> getFollowers(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            QuerySnapshot snapshot = awaitTask(followsRef.whereEqualTo("followingId", userId).get());
            List<User> followers = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Follow follow = Follow.fromMap(doc.getData(), doc.getId());
                if (follow != null && follow.getFollowerId() != null) {
                    User u = getUserProfile(follow.getFollowerId());
                    if (u != null) {
                        followers.add(u);
                    }
                }
            }
            return followers;
        } catch (Exception e) {
            Log.e(TAG, "getFollowers error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // F13: Danh sách đang theo dõi
    public List<User> getFollowing(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            QuerySnapshot snapshot = awaitTask(followsRef.whereEqualTo("followerId", userId).get());
            List<User> following = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Follow follow = Follow.fromMap(doc.getData(), doc.getId());
                if (follow != null && follow.getFollowingId() != null) {
                    User u = getUserProfile(follow.getFollowingId());
                    if (u != null) {
                        following.add(u);
                    }
                }
            }
            return following;
        } catch (Exception e) {
            Log.e(TAG, "getFollowing error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // F14: Tìm kiếm người dùng
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String lowerQuery = query.toLowerCase().trim();
            QuerySnapshot snapshot = awaitTask(usersRef.get());
            List<User> results = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                User user = User.fromMap(doc.getData(), doc.getId());
                if (user != null) {
                    String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
                    String displayName = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
                    if (username.contains(lowerQuery) || displayName.contains(lowerQuery)) {
                        results.add(user);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            Log.e(TAG, "searchUsers error: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private static <T> T awaitTask(Task<T> task) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            AtomicReference<T> resultRef = new AtomicReference<>();
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            task.addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    resultRef.set(task1.getResult());
                } else {
                    exceptionRef.set(task1.getException() != null ? task1.getException() : new RuntimeException("Task failed"));
                }
                latch.countDown();
            });

            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Operation timed out");
            }

            if (exceptionRef.get() != null) {
                throw exceptionRef.get();
            }
            return resultRef.get();
        } else {
            return Tasks.await(task, 10, TimeUnit.SECONDS);
        }
    }
}
