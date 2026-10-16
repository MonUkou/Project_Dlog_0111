package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Like extends BaseEntity {

    private String postId;
    private String userId;

    public Like() {
        super();
    }

    public Like(String postId, String userId) {
        super(generateLikeId(postId, userId));
        this.postId = postId;
        this.userId = userId;
    }

    public static String generateLikeId(String postId, String userId) {
        String p = (postId != null) ? postId.trim() : "";
        String u = (userId != null) ? userId.trim() : "";
        return p + "_" + u;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
        if (this.postId != null && this.userId != null) {
            setId(generateLikeId(this.postId, this.userId));
        }
        touch();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        if (this.postId != null && this.userId != null) {
            setId(generateLikeId(this.postId, this.userId));
        }
        touch();
    }

    @Override
    public boolean validate() {
        return postId != null && !postId.trim().isEmpty()
                && userId != null && !userId.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("postId", postId);
        map.put("userId", userId);
        return map;
    }
}