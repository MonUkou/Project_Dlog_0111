package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Comment extends BaseEntity {
    private String postId;
    private String userId;
    private String username;
    private String avatarUrl;
    private String content;

    public Comment() {
        super();
    }

    public Comment(String id, String postId, String userId, String username, String content) {
        super(id);
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.content = content;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public boolean validate() {
        return postId != null && !postId.trim().isEmpty()
                && userId != null && !userId.trim().isEmpty()
                && content != null && !content.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("postId", postId);
        map.put("userId", userId);
        map.put("username", username);
        map.put("avatarUrl", avatarUrl);
        map.put("content", content);
        return map;
    }

    public static Comment fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Comment comment = new Comment();
        if (map.containsKey("id") && map.get("id") != null) comment.setId((String) map.get("id"));
        if (map.containsKey("postId") && map.get("postId") != null) comment.setPostId((String) map.get("postId"));
        if (map.containsKey("userId") && map.get("userId") != null) comment.setUserId((String) map.get("userId"));
        if (map.containsKey("username") && map.get("username") != null) comment.setUsername((String) map.get("username"));
        if (map.containsKey("avatarUrl") && map.get("avatarUrl") != null) comment.setAvatarUrl((String) map.get("avatarUrl"));
        if (map.containsKey("content") && map.get("content") != null) comment.setContent((String) map.get("content"));
        return comment;
    }
}
