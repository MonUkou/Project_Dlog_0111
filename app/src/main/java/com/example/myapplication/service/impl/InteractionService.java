package com.example.myapplication.service.impl;

import androidx.annotation.NonNull;
import com.example.myapplication.model.Comment;
import com.example.myapplication.model.Like;
import com.example.myapplication.service.IAuthService;
import com.example.myapplication.service.INotificationService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class InteractionService {

    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_LIKES = "likes";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore dbRef;
    private AuthService authService;
    private NotificationService notificationService;

    private static InteractionService instance;

    public static synchronized InteractionService getInstance() {
        if (instance == null) {
            instance = new InteractionService();
        }
        return instance;
    }

    public InteractionService() {
        this.dbRef = FirebaseFirestore.getInstance();
        this.authService = new AuthService();
        this.notificationService = new NotificationService();
    }

    public InteractionService(AuthService authService, NotificationService notificationService) {
        this.dbRef = FirebaseFirestore.getInstance();
        this.authService = authService;
        this.notificationService = notificationService;
    }

    public interface ServiceCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void isPostLiked(String userId, String postId, final Callback<Boolean> callback) {
        isLiked(postId, new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(new Exception(message));
            }
        });
    }

    public void likePost(final String userId, final String postId, final Callback<Like> callback) {
        like(postId, new ServiceCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Like likeObj = new Like(postId, userId != null ? userId : getCurrentUid());
                if (callback != null) callback.onSuccess(likeObj);
            }

            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(new Exception(message));
            }
        });
    }

    public void unlikePost(String userId, String postId, final Callback<Void> callback) {
        unlike(postId, new ServiceCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(new Exception(message));
            }
        });
    }

    private String getCurrentUid() {
        if (authService != null) {
            if (!authService.isLoggedIn()) {
                return null;
            }
            return authService.getCurrentUserId();
        }
        return null;
    }

    private void findLike(String postId, String userId,
                          final Consumer<DocumentSnapshot> success,
                          final Consumer<Exception> failure) {
        dbRef.collection(COLLECTION_LIKES).whereEqualTo("postId", postId).whereEqualTo("userId", userId)
            .limit(1).get()
            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override public void onSuccess(QuerySnapshot result) {
                    success.accept(result.isEmpty() ? null : result.getDocuments().get(0));
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override public void onFailure(@NonNull Exception error) {
                    failure.accept(error);
                }
            });
    }

    // =========================================================================
    // F25: THÍCH BÀI VIẾT - like(postId): void
    // =========================================================================
    public void like(String postId) {
        like(postId, null);
    }

    public void like(final String postId, final ServiceCallback<Void> callback) {
        final String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để thích bài viết.");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        final Like likeObj = new Like(postId, currentUid);
        if (!likeObj.validate()) {
            if (callback != null) callback.onError("Dữ liệu lượt thích không hợp lệ.");
            return;
        }

        findLike(postId, currentUid, new Consumer<DocumentSnapshot>() {
            @Override public void accept(DocumentSnapshot existing) {
                final DocumentReference likeRef = existing == null ? dbRef.collection(COLLECTION_LIKES).document() : existing.getReference();
                likeObj.setId(likeRef.getId());
                final DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);

                dbRef.runTransaction(new Transaction.Function<String>() {
                    @Override public String apply(Transaction transaction) throws FirebaseFirestoreException {
                        DocumentSnapshot likeSnap = transaction.get(likeRef);
                        if (likeSnap.exists()) {
                            return null;
                        }

                        DocumentSnapshot postSnap = transaction.get(postRef);
                        if (!postSnap.exists()) {
                            throw new FirebaseFirestoreException("Bài viết không tồn tại.",
                                    FirebaseFirestoreException.Code.NOT_FOUND);
                        }

                        String postAuthorId = postSnap.getString("authorId");
                        Long likesCount = postSnap.getLong("likesCount");
                        long updatedLikes = (likesCount != null ? likesCount : 0L) + 1L;

                        transaction.set(likeRef, likeObj.toMap());
                        transaction.update(postRef, "likesCount", updatedLikes);

                        return postAuthorId;
                    }
                }).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override public void onSuccess(String postAuthorId) {
                        if (postAuthorId != null && !postAuthorId.equals(currentUid)) {
                            triggerLikeNotificationOnce(postAuthorId, currentUid, postId);
                        }
                        if (callback != null) callback.onSuccess(null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError("Không thể thích bài viết: " + e.getMessage());
                    }
                });
            }
        }, new Consumer<Exception>() {
            @Override public void accept(Exception error) {
                if (callback != null) callback.onError("Không kiểm tra được lượt thích: " + error.getMessage());
            }
        });
    }

    // =========================================================================
    // F26: BỎ THÍCH - unlike(postId): void
    // =========================================================================
    public void unlike(String postId) {
        unlike(postId, null);
    }

    public void unlike(final String postId, final ServiceCallback<Void> callback) {
        final String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để bỏ thích.");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        findLike(postId, currentUid, new Consumer<DocumentSnapshot>() {
            @Override public void accept(DocumentSnapshot existing) {
                if (existing == null) { if (callback != null) callback.onSuccess(null); return; }
                final DocumentReference likeRef = existing.getReference();
                final DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);

                dbRef.runTransaction(new Transaction.Function<Void>() {
                    @Override public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                        DocumentSnapshot likeSnap = transaction.get(likeRef);
                        if (!likeSnap.exists()) {
                            return null;
                        }

                        DocumentSnapshot postSnap = transaction.get(postRef);
                        if (postSnap.exists()) {
                            Long likesCount = postSnap.getLong("likesCount");
                            long updatedLikes = Math.max(0L, (likesCount != null ? likesCount : 0L) - 1L);
                            transaction.update(postRef, "likesCount", updatedLikes);
                        }

                        transaction.delete(likeRef);
                        return null;
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override public void onSuccess(Void unused) {
                        if (callback != null) callback.onSuccess(null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError("Không thể bỏ thích: " + e.getMessage());
                    }
                });
            }
        }, new Consumer<Exception>() {
            @Override public void accept(Exception error) {
                if (callback != null) callback.onError("Không kiểm tra được lượt thích: " + error.getMessage());
            }
        });
    }

    // =========================================================================
    // F27: KIỂM TRA ĐÃ THÍCH - isLiked(postId): Boolean
    // =========================================================================
    public Boolean isLiked(String postId) {
        String currentUid = getCurrentUid();
        return currentUid != null && postId != null && !postId.trim().isEmpty();
    }

    public void isLiked(String postId, final ServiceCallback<Boolean> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null || postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(false);
            return;
        }

        findLike(postId, currentUid, new Consumer<DocumentSnapshot>() {
            @Override public void accept(DocumentSnapshot document) {
                if (callback != null) callback.onSuccess(document != null);
            }
        }, new Consumer<Exception>() {
            @Override public void accept(Exception error) {
                if (callback != null) callback.onError("Lỗi kiểm tra trạng thái thích: " + error.getMessage());
            }
        });
    }

    // =========================================================================
    // F28: BÌNH LUẬN BÀI VIẾT - addComment(postId, content): Comment
    // =========================================================================
    public Comment addComment(String postId, String content) {
        String currentUid = getCurrentUid();
        if (currentUid == null) return null;
        Comment comment = new Comment(postId, currentUid, content);
        if (!comment.validate()) return null;
        addComment(postId, content, null);
        return comment;
    }

    public void addComment(final String postId, String content, final ServiceCallback<Comment> callback) {
        final String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để bình luận.");
            return;
        }

        final DocumentReference commentRef = dbRef.collection(COLLECTION_COMMENTS).document();
        final Comment comment = new Comment(postId, currentUid, content);
        comment.setId(commentRef.getId());

        if (!comment.validate()) {
            if (callback != null) callback.onError("Nội dung bình luận không được để trống.");
            return;
        }

        final DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);
        dbRef.collection("users").document(currentUid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot userSnapshot) {
                        String username = userSnapshot.getString("username");
                        String avatarUrl = userSnapshot.getString("avatarUrl");
                        comment.setUsername(username != null && !username.trim().isEmpty() ? username : currentUid);
                        comment.setAvatarUrl(avatarUrl);

                        dbRef.runTransaction(new Transaction.Function<String>() {
                            @Override public String apply(Transaction transaction) throws FirebaseFirestoreException {
                                DocumentSnapshot postSnap = transaction.get(postRef);
                                if (!postSnap.exists()) {
                                    throw new FirebaseFirestoreException("Bài viết không tồn tại.",
                                            FirebaseFirestoreException.Code.NOT_FOUND);
                                }

                                String postAuthorId = postSnap.getString("authorId");
                                Long commentsCount = postSnap.getLong("commentsCount");
                                long updatedComments = (commentsCount != null ? commentsCount : 0L) + 1L;

                                transaction.set(commentRef, comment.toMap());
                                transaction.update(postRef, "commentsCount", updatedComments);
                                return postAuthorId;
                            }
                        }).addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override public void onSuccess(String postAuthorId) {
                                if (postAuthorId != null && !postAuthorId.equals(currentUid) && notificationService != null) {
                                    notificationService.createNotification(postAuthorId, currentUid, "COMMENT", postId);
                                }
                                if (callback != null) callback.onSuccess(comment);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override public void onFailure(@NonNull Exception e) {
                                if (callback != null) callback.onError("Không thể gửi bình luận: " + e.getMessage());
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError("Không thể tải hồ sơ để bình luận: " + e.getMessage());
                    }
                });
    }

    // =========================================================================
    // F29: XEM BÌNH LUẬN - getComments(postId): List<Comment>
    // =========================================================================
    public List<Comment> getComments(String postId) {
        return new ArrayList<>();
    }

    public void getComments(String postId, final ServiceCallback<List<Comment>> callback) {
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        dbRef.collection(COLLECTION_COMMENTS)
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override public void onSuccess(QuerySnapshot querySnapshot) {
                        List<Comment> comments = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (Boolean.TRUE.equals(doc.getBoolean("hidden"))) continue;
                            Comment c = doc.toObject(Comment.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                comments.add(c);
                            }
                        }
                        Collections.sort(comments, new Comparator<Comment>() {
                            @Override public int compare(Comment c1, Comment c2) {
                                return Long.compare(c1.getCreatedAt(), c2.getCreatedAt());
                            }
                        });
                        if (callback != null) callback.onSuccess(comments);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override public void onFailure(@NonNull Exception e) {
                        if (callback != null) callback.onError("Lỗi tải bình luận: " + e.getMessage());
                    }
                });
    }

    /**
     * Observes the visible comments of one post. The first callback contains the current
     * comments and later callbacks contain additions, deletions and moderation changes.
     */
    public ListenerRegistration observeComments(String postId,
                                                 ServiceCallback<List<Comment>> callback) {
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return null;
        }

        return dbRef.collection(COLLECTION_COMMENTS)
                .whereEqualTo("postId", postId)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override public void onEvent(QuerySnapshot querySnapshot, FirebaseFirestoreException error) {
                    if (error != null) {
                        if (callback != null) {
                            callback.onError("Lỗi đồng bộ bình luận: " + error.getMessage());
                        }
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("hidden"))) continue;
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            comments.add(comment);
                        }
                    }
                    Collections.sort(comments, new Comparator<Comment>() {
                        @Override public int compare(Comment first, Comment second) {
                            return Long.compare(first.getCreatedAt(), second.getCreatedAt());
                        }
                    });
                    if (callback != null) callback.onSuccess(comments);
                    }
                });
    }

    // =========================================================================
    // F30: XÓA BÌNH LUẬN - deleteComment(commentId): void
    // =========================================================================
    public void deleteComment(String commentId) {
        deleteComment(commentId, null);
    }

    public void deleteComment(String commentId, final ServiceCallback<Void> callback) {
        final String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để xóa bình luận.");
            return;
        }
        if (commentId == null || commentId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bình luận không hợp lệ.");
            return;
        }

        final DocumentReference commentRef = dbRef.collection(COLLECTION_COMMENTS).document(commentId);

        dbRef.runTransaction(new Transaction.Function<Void>() {
            @Override public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot commentSnap = transaction.get(commentRef);
                if (!commentSnap.exists()) {
                    throw new FirebaseFirestoreException("Bình luận không tồn tại.",
                            FirebaseFirestoreException.Code.NOT_FOUND);
                }

                Comment comment = commentSnap.toObject(Comment.class);
                String postId = commentSnap.getString("postId");
                String commentAuthorId = commentSnap.getString("authorId");

                DocumentReference postRef = (postId != null && !postId.isEmpty())
                        ? dbRef.collection(COLLECTION_POSTS).document(postId)
                        : null;
                DocumentSnapshot postSnap = (postRef != null) ? transaction.get(postRef) : null;
                String postAuthorId = (postSnap != null && postSnap.exists()) ? postSnap.getString("authorId") : "";

                boolean isCommentOwner = (comment != null && comment.isOwnedBy(currentUid))
                        || currentUid.equals(commentAuthorId);
                boolean isPostOwner = currentUid.equals(postAuthorId);

                if (!isCommentOwner && !isPostOwner) {
                    throw new FirebaseFirestoreException("Bạn không có quyền xóa bình luận này.",
                            FirebaseFirestoreException.Code.PERMISSION_DENIED);
                }

                transaction.delete(commentRef);
                if (postSnap != null && postSnap.exists()) {
                    Long commentsCount = postSnap.getLong("commentsCount");
                    long updatedComments = Math.max(0L, (commentsCount != null ? commentsCount : 0L) - 1L);
                    transaction.update(postRef, "commentsCount", updatedComments);
                }
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override public void onSuccess(Void unused) {
                if (callback != null) callback.onSuccess(null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override public void onFailure(@NonNull Exception e) {
                if (callback != null) callback.onError("Không thể xóa bình luận: " + e.getMessage());
            }
        });
    }

    private void triggerLikeNotificationOnce(final String receiverId, final String senderId, final String postId) {
        if (notificationService == null) return;

        dbRef.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("type", "LIKE")
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override public void onSuccess(QuerySnapshot snapshots) {
                        if (snapshots.isEmpty()) {
                            notificationService.createNotification(receiverId, senderId, "LIKE", postId);
                        }
                    }
                });
    }
}
