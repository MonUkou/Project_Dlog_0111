package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Like extends BaseEntity {
    private String postId;
    private String userId;

    public Like() {
        super();
    }

    public Like(String userId, String postId) {
        super(userId + "_" + postId);
        this.userId = userId;
        this.postId = postId;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public static Like fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Like like = new Like();
        if (map.containsKey("id") && map.get("id") != null) like.setId((String) map.get("id"));
        if (map.containsKey("postId") && map.get("postId") != null) like.setPostId((String) map.get("postId"));
        if (map.containsKey("userId") && map.get("userId") != null) like.setUserId((String) map.get("userId"));
        return like;
    }
}
