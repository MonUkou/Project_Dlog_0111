package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class SavedPost extends BaseEntity {

    private String userId;
    private String postId;
    private String collectionName = "Chung";

    public SavedPost() {
        super();
    }

    public SavedPost(String id, String userId, String postId) {
        super(id);
        this.userId = userId;
        this.postId = postId;
    }

    public SavedPost(String userId, String postId) {
        super(userId + "_" + postId);
        this.userId = userId;
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String value) {
        collectionName = value == null || value.trim().isEmpty() ? "Chung" : value.trim();
    }

    @Override
    public boolean validate() {
        return userId != null && !userId.trim().isEmpty()
                && postId != null && !postId.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("userId", userId);
        map.put("postId", postId);
        map.put("collectionName", collectionName);
        return map;
    }

    public static SavedPost fromMap(Map<String, Object> map) {
        if (map == null) return null;
        SavedPost savedPost = new SavedPost();
        if (map.containsKey("id") && map.get("id") != null) savedPost.setId((String) map.get("id"));
        if (map.containsKey("userId") && map.get("userId") != null) savedPost.setUserId((String) map.get("userId"));
        if (map.containsKey("postId") && map.get("postId") != null) savedPost.setPostId((String) map.get("postId"));
        if (map.containsKey("collectionName") && map.get("collectionName") != null) savedPost.setCollectionName((String) map.get("collectionName"));
        return savedPost;
    }
}
