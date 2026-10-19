package com.example.myapplication.service.impl;

import com.example.myapplication.model.Comment;
import com.example.myapplication.model.Like;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class InteractionService {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private static InteractionService instance;
    private final FirebaseFirestore db;

    public InteractionService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized InteractionService getInstance() {
        if (instance == null) {
            instance = new InteractionService();
        }
        return instance;
    }

    // Like post
    public void likePost(String userId, String postId, Callback<Like> callback) {
        if (userId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = userId + "_" + postId;
        db.collection("likes").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(Like.fromMap(doc.getData()));
                        return;
                    }

                    Like like = new Like(userId, postId);
                    like.setId(docId);

                    db.collection("likes").document(docId)
                            .set(like.toMap())
                            .addOnSuccessListener(aVoid -> {
                                db.collection("posts").document(postId)
                                        .update("likesCount", FieldValue.increment(1));
                                callback.onSuccess(like);
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // Unlike post
    public void unlikePost(String userId, String postId, Callback<Void> callback) {
        if (userId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        String docId = userId + "_" + postId;
        db.collection("likes").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("posts").document(postId)
                            .update("likesCount", FieldValue.increment(-1));
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    // Is post liked by user
    public void isPostLiked(String userId, String postId, Callback<Boolean> callback) {
        if (userId == null || postId == null) {
            callback.onSuccess(false);
            return;
        }

        String docId = userId + "_" + postId;
        db.collection("likes").document(docId).get()
                .addOnSuccessListener(doc -> callback.onSuccess(doc.exists()))
                .addOnFailureListener(e -> callback.onSuccess(false));
    }

    // Add comment
    public void addComment(String userId, String username, String avatarUrl, String postId, String content, Callback<Comment> callback) {
        if (userId == null || postId == null || content == null || content.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Bình luận không được để trống."));
            return;
        }

        String commentId = db.collection("comments").document().getId();
        Comment comment = new Comment(commentId, postId, userId, username, content.trim());
        if (avatarUrl != null) comment.setAvatarUrl(avatarUrl);

        db.collection("comments").document(commentId)
                .set(comment.toMap())
                .addOnSuccessListener(aVoid -> {
                    db.collection("posts").document(postId)
                            .update("commentsCount", FieldValue.increment(1));
                    callback.onSuccess(comment);
                })
                .addOnFailureListener(callback::onError);
    }

    // Get comments for post
    public void getComments(String postId, Callback<List<Comment>> callback) {
        if (postId == null) {
            callback.onError(new IllegalArgumentException("PostId không hợp lệ."));
            return;
        }

        db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (doc.getData() != null) {
                            Comment comment = Comment.fromMap(doc.getData());
                            if (comment != null) comments.add(comment);
                        }
                    }
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    // Fallback query if index is building
                    db.collection("comments")
                            .whereEqualTo("postId", postId)
                            .get()
                            .addOnSuccessListener(querySnapshot2 -> {
                                List<Comment> comments = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot2.getDocuments()) {
                                    if (doc.getData() != null) {
                                        Comment comment = Comment.fromMap(doc.getData());
                                        if (comment != null) comments.add(comment);
                                    }
                                }
                                comments.sort((c1, c2) -> Long.compare(c1.getCreatedAt(), c2.getCreatedAt()));
                                callback.onSuccess(comments);
                            })
                            .addOnFailureListener(callback::onError);
                });
    }

    // Delete comment
    public void deleteComment(String commentId, String postId, Callback<Void> callback) {
        if (commentId == null || postId == null) {
            callback.onError(new IllegalArgumentException("Tham số không hợp lệ."));
            return;
        }

        db.collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("posts").document(postId)
                            .update("commentsCount", FieldValue.increment(-1));
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }
}
