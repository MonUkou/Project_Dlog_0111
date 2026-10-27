package com.example.myapplication.service.impl;

import androidx.annotation.NonNull;
import com.example.myapplication.model.Follow;
import com.example.myapplication.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public void getUserProfile(String userId, final Callback<User> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("UserId không hợp lệ."));
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                            User user = User.fromMap(documentSnapshot.getData());
                            user.setId(documentSnapshot.getId());
                            callback.onSuccess(user);
                        } else {
                            // Create standard fallback user for display
                            User user = new User(userId, userId, userId, userId + "@example.com", "", "Giới thiệu ngắn về bản thân.");
                            callback.onSuccess(user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Theo dõi hồ sơ theo thời gian thực. Listener nhận cả dữ liệu cache khi mất mạng
     * và dữ liệu mới ngay sau khi tài liệu users/{uid} thay đổi trên Firestore.
     */
    public ListenerRegistration observeUserProfile(String userId, final Callback<User> callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("UserId không hợp lệ."));
            return null;
        }

        return db.collection("users").document(userId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot snapshot,
                                        FirebaseFirestoreException error) {
                        if (error != null) {
                            callback.onError(error);
                            return;
                        }
                        if (snapshot == null || !snapshot.exists() || snapshot.getData() == null) {
                            callback.onError(new IllegalStateException("Không tìm thấy hồ sơ người dùng."));
                            return;
                        }
                        User user = User.fromMap(snapshot.getData());
                        if (user == null) {
                            callback.onError(new IllegalStateException("Dữ liệu hồ sơ không hợp lệ."));
                            return;
                        }
                        user.setId(snapshot.getId());
                        callback.onSuccess(user);
                    }
                });
    }

    public void updateUserProfile(final User user, final Callback<User> callback) {
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
        profileFields.put("coverUrl", user.getCoverUrl());
        profileFields.put("updatedAt", user.getUpdatedAt());
        db.collection("users").document(user.getId())
                // Only profile fields are written; role and social counters stay intact.
                .set(profileFields, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    private void findFollows(String followerId, String followingId,
                             final Callback<QuerySnapshot> callback) {
        db.collection("follows").whereEqualTo("followerId", followerId).whereEqualTo("followingId", followingId)
            .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot result) {
                    callback.onSuccess(result);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onError(e);
                }
            });
    }

    public void followUser(final String followerId, final String followingId, final Callback<Follow> callback) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            callback.onError(new IllegalArgumentException("Không thể theo dõi tài khoản này.")); return;
        }
        findFollows(followerId, followingId, new Callback<QuerySnapshot>() {
            public void onSuccess(QuerySnapshot existing) {
                if (!existing.isEmpty()) {
                    Follow follow = Follow.fromMap(existing.getDocuments().get(0).getData());
                    callback.onSuccess(follow); return;
                }
                DocumentReference ref = db.collection("follows").document();
                final Follow follow = new Follow(followerId, followingId); follow.setId(ref.getId());
                WriteBatch batch = db.batch();
                batch.set(ref, follow.toMap());
                batch.update(db.collection("users").document(followerId), "followingCount", FieldValue.increment(1));
                batch.update(db.collection("users").document(followingId), "followersCount", FieldValue.increment(1));
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        callback.onSuccess(follow);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
            }
            public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void unfollowUser(final String followerId, final String followingId, final Callback<Void> callback) {
        if (followerId == null || followingId == null) { callback.onError(new IllegalArgumentException("Vui lòng đăng nhập lại.")); return; }
        findFollows(followerId, followingId, new Callback<QuerySnapshot>() {
            public void onSuccess(QuerySnapshot existing) {
                if (existing.isEmpty()) { callback.onSuccess(null); return; }
                WriteBatch batch = db.batch();
                for (DocumentSnapshot document : existing.getDocuments()) batch.delete(document.getReference());
                batch.update(db.collection("users").document(followerId), "followingCount", FieldValue.increment(-existing.size()));
                batch.update(db.collection("users").document(followingId), "followersCount", FieldValue.increment(-existing.size()));
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        callback.onSuccess(null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
            }
            public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void isFollowing(String followerId, String followingId, final Callback<Boolean> callback) {
        if (followerId == null || followingId == null) { callback.onSuccess(false); return; }
        findFollows(followerId, followingId, new Callback<QuerySnapshot>() {
            public void onSuccess(QuerySnapshot result) { callback.onSuccess(!result.isEmpty()); }
            public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void getFollowUsers(String userId, final boolean following, final Callback<List<User>> callback) {
        if (userId == null || userId.isEmpty()) { callback.onError(new IllegalArgumentException("Không tìm thấy hồ sơ.")); return; }
        db.collection("follows").whereEqualTo(following ? "followerId" : "followingId", userId).get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot relations) {
                    Set<String> ids = new LinkedHashSet<>();
                    for (DocumentSnapshot relation : relations.getDocuments()) {
                        String id = relation.getString(following ? "followingId" : "followerId");
                        if (id != null && !id.isEmpty()) ids.add(id);
                    }
                    if (ids.isEmpty()) { callback.onSuccess(new ArrayList<User>()); return; }
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String id : ids) tasks.add(db.collection("users").document(id).get());
                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                        @Override
                        public void onSuccess(List<Object> results) {
                            List<User> users = new ArrayList<>();
                            for (Object object : results) {
                                DocumentSnapshot document = (DocumentSnapshot)object;
                                if (document.exists() && document.getData() != null) {
                                    User user = User.fromMap(document.getData()); user.setId(document.getId()); users.add(user);
                                }
                            }
                            final Collator collator = Collator.getInstance(new Locale("vi"));
                            Collections.sort(users, new Comparator<User>() {
                                @Override
                                public int compare(User left, User right) {
                                    return collator.compare(left.getFullName() == null ? "" : left.getFullName(), right.getFullName() == null ? "" : right.getFullName());
                                }
                            });
                            callback.onSuccess(users);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callback.onError(e);
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onError(e);
                }
            });
    }

    public void searchUsers(String queryText, final Callback<List<User>> callback) {
        if (queryText == null || queryText.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<User>());
            return;
        }

        db.collection("users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<User> users = new ArrayList<>();
                        String searchLower = queryText.trim().toLowerCase();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            if (doc.getData() != null) {
                                User u = User.fromMap(doc.getData());
                                if (u != null) u.setId(doc.getId());
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
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }
}
