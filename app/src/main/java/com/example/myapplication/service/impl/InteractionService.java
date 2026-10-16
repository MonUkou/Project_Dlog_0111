package com.example.myapplication.service.impl;

import com.example.myapplication.model.Comment;
import com.example.myapplication.model.Like;
import com.example.myapplication.service.IAuthService;
import com.example.myapplication.service.INotificationService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InteractionService {

    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_LIKES = "likes";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore dbRef;
    private AuthService authService;
    private NotificationService notificationService;

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

    private String getCurrentUid() {
        if (authService != null) {
            if (!authService.isLoggedIn()) {
                return null;
            }
            return authService.getCurrentUserId();
        }
        return null;
    }

    // =========================================================================
    // F25: THÍCH BÀI VIẾT - like(postId): void
    // =========================================================================
    public void like(String postId) {
        like(postId, null);
    }

    public void like(String postId, ServiceCallback<Void> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để thích bài viết.");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        Like likeObj = new Like(postId, currentUid);
        if (!likeObj.validate()) {
            if (callback != null) callback.onError("Dữ liệu lượt thích không hợp lệ.");
            return;
        }

        DocumentReference likeRef = dbRef.collection(COLLECTION_LIKES).document(likeObj.getId());
        DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);

        dbRef.runTransaction(transaction -> {
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
        }).addOnSuccessListener(postAuthorId -> {
            if (postAuthorId != null && !postAuthorId.equals(currentUid)) {
                triggerLikeNotificationOnce(postAuthorId, currentUid, postId);
            }
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Không thể thích bài viết: " + e.getMessage());
        });
    }

    // =========================================================================
    // F26: BỎ THÍCH - unlike(postId): void
    // =========================================================================
    public void unlike(String postId) {
        unlike(postId, null);
    }

    public void unlike(String postId, ServiceCallback<Void> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để bỏ thích.");
            return;
        }
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        String likeId = Like.generateLikeId(postId, currentUid);
        DocumentReference likeRef = dbRef.collection(COLLECTION_LIKES).document(likeId);
        DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);

        dbRef.runTransaction(transaction -> {
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
        }).addOnSuccessListener(unused -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Không thể bỏ thích: " + e.getMessage());
        });
    }

    // =========================================================================
    // F27: KIỂM TRA ĐÃ THÍCH - isLiked(postId): Boolean
    // =========================================================================
    public Boolean isLiked(String postId) {
        String currentUid = getCurrentUid();
        return currentUid != null && postId != null && !postId.trim().isEmpty();
    }

    public void isLiked(String postId, ServiceCallback<Boolean> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null || postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(false);
            return;
        }

        String likeId = Like.generateLikeId(postId, currentUid);
        dbRef.collection(COLLECTION_LIKES).document(likeId).get()
                .addOnSuccessListener(doc -> {
                    if (callback != null) callback.onSuccess(doc.exists());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Lỗi kiểm tra trạng thái thích: " + e.getMessage());
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

    public void addComment(String postId, String content, ServiceCallback<Comment> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để bình luận.");
            return;
        }

        DocumentReference commentRef = dbRef.collection(COLLECTION_COMMENTS).document();
        Comment comment = new Comment(postId, currentUid, content);
        comment.setId(commentRef.getId());

        if (!comment.validate()) {
            if (callback != null) callback.onError("Nội dung bình luận không được để trống.");
            return;
        }

        DocumentReference postRef = dbRef.collection(COLLECTION_POSTS).document(postId);

        dbRef.runTransaction(transaction -> {
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
        }).addOnSuccessListener(postAuthorId -> {
            if (postAuthorId != null && !postAuthorId.equals(currentUid) && notificationService != null) {
                notificationService.createNotification(postAuthorId, currentUid, "COMMENT", postId);
            }
            if (callback != null) callback.onSuccess(comment);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Không thể gửi bình luận: " + e.getMessage());
        });
    }

    // =========================================================================
    // F29: XEM BÌNH LUẬN - getComments(postId): List<Comment>
    // =========================================================================
    public List<Comment> getComments(String postId) {
        return new ArrayList<>();
    }

    public void getComments(String postId, ServiceCallback<List<Comment>> callback) {
        if (postId == null || postId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bài viết không hợp lệ.");
            return;
        }

        dbRef.collection(COLLECTION_COMMENTS)
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Comment c = doc.toObject(Comment.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            comments.add(c);
                        }
                    }
                    Collections.sort(comments, (c1, c2) -> Long.compare(c1.getCreatedAt(), c2.getCreatedAt()));
                    if (callback != null) callback.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Lỗi tải bình luận: " + e.getMessage());
                });
    }

    // =========================================================================
    // F30: XÓA BÌNH LUẬN - deleteComment(commentId): void
    // =========================================================================
    public void deleteComment(String commentId) {
        deleteComment(commentId, null);
    }

    public void deleteComment(String commentId, ServiceCallback<Void> callback) {
        String currentUid = getCurrentUid();
        if (currentUid == null) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để xóa bình luận.");
            return;
        }
        if (commentId == null || commentId.trim().isEmpty()) {
            if (callback != null) callback.onError("Mã bình luận không hợp lệ.");
            return;
        }

        DocumentReference commentRef = dbRef.collection(COLLECTION_COMMENTS).document(commentId);

        dbRef.runTransaction(transaction -> {
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
        }).addOnSuccessListener(unused -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Không thể xóa bình luận: " + e.getMessage());
        });
    }

    private void triggerLikeNotificationOnce(String receiverId, String senderId, String postId) {
        if (notificationService == null) return;

        dbRef.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("type", "LIKE")
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        notificationService.createNotification(receiverId, senderId, "LIKE", postId);
                    }
                });
    }
}